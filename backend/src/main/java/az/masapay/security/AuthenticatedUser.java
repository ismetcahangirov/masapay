package az.masapay.security;

import az.masapay.domain.enums.UserRole;
import java.util.UUID;

/** Principal placed in the SecurityContext, derived from a verified access token. */
public record AuthenticatedUser(UUID id, String email, UserRole role) {
}
