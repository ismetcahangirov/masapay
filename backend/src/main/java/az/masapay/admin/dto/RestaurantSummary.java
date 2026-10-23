package az.masapay.admin.dto;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A restaurant the signed-in admin may manage, for the admin panel's restaurant picker, with where
 * its subscription stands today so the panel can warn the manager before and after it ends (#148).
 */
public record RestaurantSummary(UUID id, String name, SubscriptionStatus subscriptionStatus,
		LocalDate subscriptionEndsOn) {

	public static RestaurantSummary from(Restaurant restaurant, LocalDate today) {
		return new RestaurantSummary(restaurant.getId(), restaurant.getName(), restaurant.subscriptionStatusOn(today),
			restaurant.getSubscriptionEndsOn());
	}
}
