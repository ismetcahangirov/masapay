package az.masapay.auth.dto;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.util.UUID;

/**
 * Result of a Google login. When {@code enabled} is false the account is
 * provisioned but pending admin activation; JWT issuance for enabled users is
 * added in #15.
 */
public record GoogleLoginResponse(UUID userId, String email, String name, boolean enabled, UserRole role) {

	public static GoogleLoginResponse from(User user) {
		return new GoogleLoginResponse(user.getId(), user.getEmail(), user.getName(), user.isEnabled(), user.getRole());
	}
}
