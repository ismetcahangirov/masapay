package az.masapay.restaurant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.SubscriptionPlan;
import az.masapay.repository.RestaurantTableRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RestaurantGateTest {

	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final UUID qrToken = UUID.randomUUID();
	private final Restaurant restaurant = new Restaurant();

	/** 2026-10-22 21:00 UTC is already 2026-10-23 01:00 in Baku. */
	private final RestaurantGate gate = new RestaurantGate(tableRepository,
			Clock.fixed(Instant.parse("2026-10-22T21:00:00Z"), ZoneOffset.UTC));

	private void subscription(boolean trial, LocalDate endsOn) {
		restaurant.setSubscriptionPlan(SubscriptionPlan.BASIC);
		restaurant.setSubscriptionTrial(trial);
		restaurant.setSubscriptionStartsOn(endsOn.minusMonths(1));
		restaurant.setSubscriptionEndsOn(endsOn);
		when(tableRepository.findRestaurantByQrToken(qrToken)).thenReturn(Optional.of(restaurant));
	}

	@Test
	void anExpiredSubscriptionTakesNoPayments() {
		subscription(false, LocalDate.of(2026, 10, 22));

		assertThatThrownBy(() -> gate.checkTakesPayments(qrToken))
			.isInstanceOfSatisfying(ResponseStatusException.class, e -> {
				assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(e.getReason()).isEqualTo(RestaurantGate.SUBSCRIPTION_EXPIRED);
			});
	}

	@Test
	void theLastDayStillTakesPaymentsInBakuTime() {
		subscription(false, LocalDate.of(2026, 10, 23));

		assertThatCode(() -> gate.checkTakesPayments(qrToken)).doesNotThrowAnyException();
	}

	@Test
	void aTrialOrNoSubscriptionTakesPayments() {
		subscription(true, LocalDate.of(2026, 11, 1));
		assertThatCode(() -> gate.checkTakesPayments(qrToken)).doesNotThrowAnyException();

		restaurant.setSubscriptionPlan(null);
		assertThatCode(() -> gate.checkTakesPayments(qrToken)).doesNotThrowAnyException();
	}

	@Test
	void aSwitchedOffRestaurantIsClosedBeforeItsSubscriptionIsLookedAt() {
		subscription(false, LocalDate.of(2026, 1, 1));
		restaurant.setActive(false);

		assertThatThrownBy(() -> gate.checkTakesPayments(qrToken))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getReason()).isEqualTo(RestaurantGate.CLOSED));
	}
}
