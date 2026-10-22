package az.masapay.staff.dto;

import az.masapay.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Invite a waiter or chef by the email of their Google account (#145). */
public record InviteStaffRequest(@NotBlank @Email @Size(max = 320) String email, @NotNull UserRole role) {
}
