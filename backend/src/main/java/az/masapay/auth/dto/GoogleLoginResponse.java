package az.masapay.auth.dto;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.util.UUID;

/**
 * Result of a Google login. When the account is enabled, {@code tokens} carries a
 * fresh access/refresh pair; when it is still pending admin activation, tokens is
 * null and {@code enabled} is false.
 */
public record GoogleLoginResponse(
		UUID userId, String email, String name, boolean enabled, UserRole role, AuthTokens tokens) {

	public static GoogleLoginResponse from(User user, AuthTokens tokens) {
		return new GoogleLoginResponse(
			user.getId(), user.getEmail(), user.getName(), user.isEnabled(), user.getRole(), tokens);
	}
}
