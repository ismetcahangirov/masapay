package az.masapay.platform.dto;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.SubscriptionPlan;
import az.masapay.domain.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A restaurant's subscription and where it stands today (#148); fields are null for NONE. */
public record SubscriptionView(SubscriptionStatus status, SubscriptionPlan plan, boolean trial, LocalDate startsOn,
		LocalDate endsOn, BigDecimal monthlyFee) {

	public static SubscriptionView of(Restaurant restaurant, LocalDate today) {
		return new SubscriptionView(restaurant.subscriptionStatusOn(today), restaurant.getSubscriptionPlan(),
			restaurant.isSubscriptionTrial(), restaurant.getSubscriptionStartsOn(), restaurant.getSubscriptionEndsOn(),
			restaurant.getSubscriptionMonthlyFee());
	}
}
