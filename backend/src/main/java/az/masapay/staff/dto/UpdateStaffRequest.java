package az.masapay.staff.dto;

import az.masapay.domain.enums.UserRole;

/**
 * Change a staff member (#145): {@code role} WAITER or CHEF, {@code active} true approves or
 * re-activates, false switches the account off. Null fields stay as they are.
 */
public record UpdateStaffRequest(UserRole role, Boolean active) {
}
