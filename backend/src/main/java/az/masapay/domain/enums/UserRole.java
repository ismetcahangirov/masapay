package az.masapay.domain.enums;

/** RBAC role of an application user. Matches the users.role CHECK constraint; enforced in #16. */
public enum UserRole {
	SUPER_ADMIN,
	RESTO_MANAGER,
	WAITER,
	/** Kitchen staff: sees ordered dishes and marks them preparing / ready (#142). */
	CHEF
}
