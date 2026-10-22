package az.masapay.staff.dto;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

/** A waiter or chef as the restaurant manager sees them (#145). */
public record StaffMemberView(UUID id, String email, String name, UserRole role, Status status,
		Instant lastLoginAt) {

	public enum Status {
		/** Invited by email, has not signed in yet. */
		INVITED,
		/** Signed in with Google, waiting for the manager's approval. */
		PENDING,
		ACTIVE,
		/** Switched off by the manager. */
		DEACTIVATED
	}

	public static StaffMemberView of(User user) {
		Status status = user.isEnabled() ? Status.ACTIVE
			: user.getDeactivatedAt() != null ? Status.DEACTIVATED
			: user.getGoogleSub() == null ? Status.INVITED
			: Status.PENDING;
		return new StaffMemberView(user.getId(), user.getEmail(), user.getName(), user.getRole(), status,
			user.getLastLoginAt());
	}
}
