package az.masapay.tip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.tip.dto.MyTipLine;
import az.masapay.tip.dto.MyTips;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MyTipsServiceTest {

	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	/** 2026-10-23 12:00 in Baku. */
	private final MyTipsService service = new MyTipsService(transactionRepository, userRepository,
			Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC));

	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);

	private void waiterOf(String currency) {
		Restaurant restaurant = new Restaurant();
		restaurant.setCurrency(currency);
		User user = new User();
		user.setId(waiter.id());
		user.setRestaurant(restaurant);
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(user));
	}

	@Test
	void aWaiterSeesOwnTipsWithTheTableThisMonthByDefault() {
		waiterOf("AZN");
		when(transactionRepository.findTipsOfWaiter(eq(waiter.id()), any(), any())).thenReturn(List.of(
				new MyTipLine(UUID.randomUUID(), Instant.parse("2026-10-22T18:00:00Z"), "Masa 4",
						new BigDecimal("3.00"), new BigDecimal("30.00")),
				new MyTipLine(UUID.randomUUID(), Instant.parse("2026-10-20T18:00:00Z"), "Masa 1",
						new BigDecimal("2.50"), new BigDecimal("25.00"))));

		MyTips tips = service.mine(waiter, null, null);

		assertThat(tips.from()).isEqualTo(LocalDate.of(2026, 10, 1));
		assertThat(tips.to()).isEqualTo(LocalDate.of(2026, 10, 23));
		assertThat(tips.tipCount()).isEqualTo(2);
		assertThat(tips.tipTotal()).isEqualByComparingTo("5.50");
		assertThat(tips.tips()).extracting(MyTipLine::tableLabel).containsExactly("Masa 4", "Masa 1");
		// Only this waiter's tips, from Baku midnight to Baku midnight.
		verify(transactionRepository).findTipsOfWaiter(waiter.id(), Instant.parse("2026-09-30T20:00:00Z"),
				Instant.parse("2026-10-23T20:00:00Z"));
	}

	@Test
	void onlyWaitersAndOnlyAValidRange() {
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		assertThatThrownBy(() -> service.mine(manager, null, null))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

		waiterOf("AZN");
		assertThatThrownBy(() -> service.mine(waiter, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}
}
