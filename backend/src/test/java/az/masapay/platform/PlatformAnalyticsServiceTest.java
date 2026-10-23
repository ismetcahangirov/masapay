package az.masapay.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.SubscriptionPlan;
import az.masapay.domain.enums.SubscriptionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.platform.dto.PlatformAnalytics;
import az.masapay.platform.dto.RestaurantPayments;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PlatformAnalyticsServiceTest {

	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	/** 2026-10-23 12:00 in Baku. */
	private final PlatformAnalyticsService service = new PlatformAnalyticsService(restaurantRepository,
			transactionRepository, Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC));

	private final AuthenticatedUser superAdmin =
			new AuthenticatedUser(UUID.randomUUID(), "admin@masapay.az", UserRole.SUPER_ADMIN);

	private static Restaurant restaurant(String name, String currency) {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		restaurant.setName(name);
		restaurant.setCurrency(currency);
		return restaurant;
	}

	private static void paid(Restaurant restaurant, boolean trial, String fee, LocalDate startsOn,
			LocalDate endsOn) {
		restaurant.setSubscriptionPlan(SubscriptionPlan.BASIC);
		restaurant.setSubscriptionTrial(trial);
		restaurant.setSubscriptionMonthlyFee(new BigDecimal(fee));
		restaurant.setSubscriptionStartsOn(startsOn);
		restaurant.setSubscriptionEndsOn(endsOn);
	}

	@Test
	void restaurantsAreComparedAndTotalledPerCurrency() {
		Restaurant dolma = restaurant("Dolma", "AZN");
		paid(dolma, false, "60.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
		Restaurant kafe = restaurant("Nərgiz Kafe", "AZN");
		kafe.setActive(false);
		Restaurant lira = restaurant("Lira", "TRY");
		when(restaurantRepository.findAll()).thenReturn(List.of(kafe, dolma, lira));
		when(transactionRepository.sumSuccessfulPaymentsByRestaurant(any(), any())).thenReturn(List.of(
				new RestaurantPayments(dolma.getId(), 3, new BigDecimal("100.00"), new BigDecimal("10.00")),
				new RestaurantPayments(lira.getId(), 1, new BigDecimal("500.00"), null)));

		PlatformAnalytics analytics = service.analytics(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
				superAdmin);

		// Biggest turnover first; "Lira" is in another currency but still a row.
		assertThat(analytics.restaurants()).extracting(PlatformAnalytics.Row::name)
				.containsExactly("Lira", "Dolma", "Nərgiz Kafe");
		PlatformAnalytics.Row dolmaRow = analytics.restaurants().get(1);
		assertThat(dolmaRow.averagePayment()).isEqualByComparingTo("33.33");
		assertThat(dolmaRow.subscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		// 15 paid days of a 60.00 monthly fee.
		assertThat(dolmaRow.subscriptionRevenue()).isEqualByComparingTo("30.00");
		assertThat(analytics.restaurants().get(2).averagePayment()).isNull();

		assertThat(analytics.totals()).extracting(PlatformAnalytics.Totals::currency).containsExactly("AZN", "TRY");
		PlatformAnalytics.Totals azn = analytics.totals().get(0);
		assertThat(azn.restaurantCount()).isEqualTo(2);
		assertThat(azn.activeRestaurantCount()).isEqualTo(1);
		assertThat(azn.paymentCount()).isEqualTo(3);
		assertThat(azn.revenue()).isEqualByComparingTo("100.00");
		assertThat(azn.tips()).isEqualByComparingTo("10.00");
		assertThat(azn.subscriptionRevenue()).isEqualByComparingTo("30.00");
		assertThat(analytics.totals().get(1).revenue()).isEqualByComparingTo("500.00");
	}

	@Test
	void theRangeIsBakuDaysFromMidnightToMidnight() {
		when(restaurantRepository.findAll()).thenReturn(List.of());

		PlatformAnalytics analytics = service.analytics(null, null, superAdmin);

		assertThat(analytics.from()).isEqualTo(LocalDate.of(2026, 9, 24));
		assertThat(analytics.to()).isEqualTo(LocalDate.of(2026, 10, 23));
		verify(transactionRepository).sumSuccessfulPaymentsByRestaurant(Instant.parse("2026-09-23T20:00:00Z"),
				Instant.parse("2026-10-23T20:00:00Z"));
	}

	@Test
	void subscriptionIncomeCountsOnlyPaidDaysInsideTheRange() {
		Restaurant dolma = restaurant("Dolma", "AZN");
		LocalDate from = LocalDate.of(2026, 10, 1);
		LocalDate to = LocalDate.of(2026, 10, 31);

		paid(dolma, false, "30.00", LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 20));
		assertThat(PlatformAnalyticsService.subscriptionRevenue(dolma, from, to)).isEqualByComparingTo("11.00");

		paid(dolma, false, "30.00", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
		assertThat(PlatformAnalyticsService.subscriptionRevenue(dolma, from, to)).isEqualByComparingTo("0.00");

		paid(dolma, true, "30.00", from, to);
		assertThat(PlatformAnalyticsService.subscriptionRevenue(dolma, from, to)).isEqualByComparingTo("0.00");
	}

	@Test
	void aBadRangeOrAnotherRoleIsRefused() {
		assertThatThrownBy(() -> service.analytics(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), superAdmin))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> service.analytics(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 1), superAdmin))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> service.analytics(null, null,
				new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER)))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}
}
