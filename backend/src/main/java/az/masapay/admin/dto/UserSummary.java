package az.masapay.admin.dto;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

/** Administrative view of a user. */
public record UserSummary(
		UUID id, String email, String name, UserRole role, boolean enabled, UUID restaurantId, Instant lastLoginAt) {

	public static UserSummary from(User user) {
		UUID restaurantId = user.getRestaurant() != null ? user.getRestaurant().getId() : null;
		return new UserSummary(
			user.getId(), user.getEmail(), user.getName(), user.getRole(),
			user.isEnabled(), restaurantId, user.getLastLoginAt());
	}
}
