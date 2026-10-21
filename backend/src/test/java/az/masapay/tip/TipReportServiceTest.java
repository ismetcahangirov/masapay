package az.masapay.tip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.tip.dto.TipReport;
import az.masapay.tip.dto.WaiterTip;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TipReportServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private RestaurantRepository restaurantRepository;

	@Mock
	private UserRepository userRepository;

	private TipReportService service;

	private final UUID restaurantId = UUID.randomUUID();
	private final Restaurant restaurant = new Restaurant();
	private final AuthenticatedUser admin =
		new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", UserRole.SUPER_ADMIN);

	@BeforeEach
	void setUp() {
		// 2026-10-31T21:30Z is already 2026-11-01 01:30 in Baku (UTC+4).
		Clock clock = Clock.fixed(Instant.parse("2026-10-31T21:30:00Z"), ZoneOffset.UTC);
		service = new TipReportService(transactionRepository, restaurantRepository, userRepository, clock);
		restaurant.setId(restaurantId);
		restaurant.setCurrency("AZN");
	}

	private void stubRestaurant() {
		when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
	}

	@Test
	void inclusiveDatesBecomeHalfOpenBakuRange() {
		stubRestaurant();

		service.report(admin, restaurantId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

		verify(transactionRepository).sumTipsByWaiter(restaurantId, TransactionStatus.SUCCESS,
			Instant.parse("2026-09-30T20:00:00Z"), Instant.parse("2026-10-31T20:00:00Z"));
	}

	@Test
	void singleDayCoversTheWholeDay() {
		stubRestaurant();

		service.report(admin, restaurantId, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5));

		verify(transactionRepository).sumTipsByWaiter(restaurantId, TransactionStatus.SUCCESS,
			Instant.parse("2026-10-04T20:00:00Z"), Instant.parse("2026-10-05T20:00:00Z"));
	}

	@Test
	void missingDatesDefaultToCurrentBakuMonth() {
		stubRestaurant();

		TipReport report = service.report(admin, restaurantId, null, null);

		assertThat(report.from()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(report.to()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(report.timeZone()).isEqualTo("Asia/Baku");
	}

	@Test
	void fromAfterToIsBadRequest() {
		assertThatThrownBy(() -> service.report(admin, restaurantId,
			LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void managerOfAnotherRestaurantIsForbidden() {
		AuthenticatedUser manager = managerOf(UUID.randomUUID());

		assertThatThrownBy(() -> service.report(manager, restaurantId, null, null))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void managerSeesOwnRestaurant() {
		AuthenticatedUser manager = managerOf(restaurantId);
		stubRestaurant();

		TipReport report = service.report(manager, restaurantId, null, null);

		assertThat(report.restaurantId()).isEqualTo(restaurantId);
	}

	@Test
	void waiterIsForbidden() {
		AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@example.com", UserRole.WAITER);

		assertThatThrownBy(() -> service.report(waiter, restaurantId, null, null))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void unknownRestaurantIsNotFound() {
		when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.report(admin, restaurantId, null, null))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void totalsSumAllWaitersIncludingUnassigned() {
		stubRestaurant();
		WaiterTip aysel = new WaiterTip(UUID.randomUUID(), "Aysel", "aysel@example.com", 3, new BigDecimal("12.50"));
		WaiterTip unassigned = new WaiterTip(null, null, null, 2, new BigDecimal("4.00"));
		when(transactionRepository.sumTipsByWaiter(eq(restaurantId), eq(TransactionStatus.SUCCESS), any(), any()))
			.thenReturn(List.of(aysel, unassigned));

		TipReport report = service.report(admin, restaurantId, null, null);

		assertThat(report.waiters()).containsExactly(aysel, unassigned);
		assertThat(report.totalTipCount()).isEqualTo(5);
		assertThat(report.totalTipAmount()).isEqualByComparingTo("16.50");
		assertThat(report.currency()).isEqualTo("AZN");
	}

	@Test
	void emptyPeriodHasZeroTotals() {
		stubRestaurant();

		TipReport report = service.report(admin, restaurantId, null, null);

		assertThat(report.waiters()).isEmpty();
		assertThat(report.totalTipCount()).isZero();
		assertThat(report.totalTipAmount()).isEqualByComparingTo("0");
	}

	private AuthenticatedUser managerOf(UUID managedRestaurantId) {
		Restaurant managed = new Restaurant();
		managed.setId(managedRestaurantId);
		User user = new User();
		user.setRestaurant(managed);
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@example.com", UserRole.RESTO_MANAGER);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));
		return manager;
	}
}
