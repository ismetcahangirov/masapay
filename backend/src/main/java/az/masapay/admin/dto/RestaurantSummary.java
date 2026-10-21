package az.masapay.admin.dto;

import az.masapay.domain.Restaurant;
import java.util.UUID;

/** A restaurant the signed-in admin may manage, for the admin panel's restaurant picker. */
public record RestaurantSummary(UUID id, String name) {

	public static RestaurantSummary from(Restaurant restaurant) {
		return new RestaurantSummary(restaurant.getId(), restaurant.getName());
	}
}
