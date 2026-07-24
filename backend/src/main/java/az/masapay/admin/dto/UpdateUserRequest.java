package az.masapay.admin.dto;

import az.masapay.domain.enums.UserRole;
import java.util.UUID;

/**
 * Partial update of a user's provisioning. Null fields are left unchanged, so an
 * admin can activate an account and assign a role and restaurant in one call.
 */
public record UpdateUserRequest(Boolean enabled, UserRole role, UUID restaurantId) {
}
