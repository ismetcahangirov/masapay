package az.masapay.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.dashboard.dto.DailyRevenue;
import az.masapay.dashboard.dto.DashboardMetrics;
import az.masapay.dashboard.dto.PaidAmount;
import az.masapay.dashboard.dto.PaymentTotals;
import az.masapay.dashboard.dto.StatusCount;
import az.masapay.dashboard.dto.TableVisit;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
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
class DashboardServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private RestaurantRepository restaurantRepository;

	@Mock
	private UserRepository userRepository;

	private DashboardService service;

	private final UUID restaurantId = UUID.randomUUID();
	private final Restaurant restaurant = new Restaurant();
	private final AuthenticatedUser admin =
		new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", UserRole.SUPER_ADMIN);

	// 2026-10-20T21:30Z is already 2026-10-21 01:30 in Baku (UTC+4).
	private final Instant todayStart = Instant.parse("2026-10-20T20:00:00Z");
	private final Instant tomorrowStart = Instant.parse("2026-10-21T20:00:00Z");

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-10-20T21:30:00Z"), ZoneOffset.UTC);
		service = new DashboardService(transactionRepository, orderRepository, restaurantRepository,
			userRepository, clock);
		restaurant.setId(restaurantId);
		restaurant.setCurrency("AZN");
	}

	private void stubRestaurant() {
		when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
	}

	@Test
	void todayIsTheBakuDayAsHalfOpenRange() {
		stubRestaurant();

		DashboardMetrics metrics = service.metrics(admin, restaurantId, 7);

		assertThat(metrics.date()).isEqualTo(LocalDate.of(2026, 10, 21));
		assertThat(metrics.timeZone()).isEqualTo("Asia/Baku");
		verify(transactionRepository).sumSuccessfulPayments(restaurantId, todayStart, tomorrowStart);
		verify(orderRepository).findPaidVisits(restaurantId, todayStart, tomorrowStart);
		verify(transactionRepository).countPayriffByStatus(restaurantId, todayStart, tomorrowStart);
		verify(transactionRepository).findSuccessfulPayments(restaurantId,
			Instant.parse("2026-10-14T20:00:00Z"), tomorrowStart);
	}

	@Test
	void numbersAreMapped() {
		stubRestaurant();
		when(transactionRepository.sumSuccessfulPayments(restaurantId, todayStart, tomorrowStart))
			.thenReturn(new PaymentTotals(new BigDecimal("125.50"), new BigDecimal("12.00")));
		when(orderRepository.findPaidVisits(restaurantId, todayStart, tomorrowStart)).thenReturn(List.of(
			new TableVisit(Instant.parse("2026-10-21T08:00:00Z"), Instant.parse("2026-10-21T08:30:00Z")),
			new TableVisit(Instant.parse("2026-10-21T09:00:00Z"), Instant.parse("2026-10-21T10:00:00Z"))));
		when(transactionRepository.countPayriffByStatus(restaurantId, todayStart, tomorrowStart)).thenReturn(List.of(
			new StatusCount(TransactionStatus.SUCCESS, 3),
			new StatusCount(TransactionStatus.FAILED, 1),
			new StatusCount(TransactionStatus.PENDING, 5)));

		DashboardMetrics metrics = service.metrics(admin, restaurantId, 7);

		assertThat(metrics.currency()).isEqualTo("AZN");
		assertThat(metrics.revenueToday()).isEqualByComparingTo("125.50");
		assertThat(metrics.tipsToday()).isEqualByComparingTo("12.00");
		assertThat(metrics.averageTableMinutes()).isEqualTo(45.0);
		assertThat(metrics.payriffSuccessRate()).isEqualTo(0.75);
		assertThat(metrics.payriffSuccessCount()).isEqualTo(3);
		assertThat(metrics.payriffFailedCount()).isEqualTo(1);
	}

	@Test
	void emptyDayHasZeroMoneyAndNullRates() {
		stubRestaurant();
		when(transactionRepository.sumSuccessfulPayments(any(), any(), any()))
			.thenReturn(new PaymentTotals(null, null));

		DashboardMetrics metrics = service.metrics(admin, restaurantId, 7);

		assertThat(metrics.revenueToday()).isEqualByComparingTo("0");
		assertThat(metrics.tipsToday()).isEqualByComparingTo("0");
		assertThat(metrics.averageTableMinutes()).isNull();
		assertThat(metrics.payriffSuccessRate()).isNull();
	}

	@Test
	void successRateIsNullWithoutPayriffOutcome() {
		stubRestaurant();
		when(transactionRepository.countPayriffByStatus(restaurantId, todayStart, tomorrowStart))
			.thenReturn(List.of(new StatusCount(TransactionStatus.PENDING, 2)));

		DashboardMetrics metrics = service.metrics(admin, restaurantId, 7);

		assertThat(metrics.payriffSuccessRate()).isNull();
	}

	@Test
	void seriesHasOneEntryPerBakuDayWithZeroGaps() {
		stubRestaurant();
		when(transactionRepository.findSuccessfulPayments(eq(restaurantId), any(), eq(tomorrowStart)))
			.thenReturn(List.of(
				// 2026-10-19 23:30 Baku
				new PaidAmount(Instant.parse("2026-10-19T19:30:00Z"), new BigDecimal("10.00"), new BigDecimal("1.00")),
				// 2026-10-20 00:30 Baku: the next day, although the same UTC date
				new PaidAmount(Instant.parse("2026-10-19T20:30:00Z"), new BigDecimal("20.00"), BigDecimal.ZERO),
				new PaidAmount(Instant.parse("2026-10-20T21:00:00Z"), new BigDecimal("5.00"), new BigDecimal("0.50"))));

		DashboardMetrics metrics = service.metrics(admin, restaurantId, 3);

		assertThat(metrics.series()).extracting(DailyRevenue::date).containsExactly(
			LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21));
		assertThat(metrics.series().get(0).revenue()).isEqualByComparingTo("10.00");
		assertThat(metrics.series().get(0).tips()).isEqualByComparingTo("1.00");
		assertThat(metrics.series().get(1).revenue()).isEqualByComparingTo("20.00");
		assertThat(metrics.series().get(2).revenue()).isEqualByComparingTo("5.00");
		assertThat(metrics.series().get(2).tips()).isEqualByComparingTo("0.50");
	}

	@Test
	void daysOutOfRangeIsBadRequest() {
		assertThatThrownBy(() -> service.metrics(admin, restaurantId, 32))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> service.metrics(admin, restaurantId, 0))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verifyNoInteractions(transactionRepository, orderRepository);
	}

	@Test
	void managerOfAnotherRestaurantIsForbidden() {
		AuthenticatedUser manager = managerOf(UUID.randomUUID());

		assertThatThrownBy(() -> service.metrics(manager, restaurantId, 7))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(transactionRepository, orderRepository);
	}

	@Test
	void managerSeesOwnRestaurant() {
		AuthenticatedUser manager = managerOf(restaurantId);
		stubRestaurant();

		DashboardMetrics metrics = service.metrics(manager, restaurantId, 7);

		assertThat(metrics.restaurantId()).isEqualTo(restaurantId);
		assertThat(metrics.series()).hasSize(7);
	}

	@Test
	void waiterIsForbidden() {
		AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@example.com", UserRole.WAITER);

		assertThatThrownBy(() -> service.metrics(waiter, restaurantId, 7))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void unknownRestaurantIsNotFound() {
		when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.metrics(admin, restaurantId, 7))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
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
