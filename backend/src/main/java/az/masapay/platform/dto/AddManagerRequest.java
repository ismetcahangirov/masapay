package az.masapay.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One more manager for a restaurant, by the email of their Google account (#147). */
public record AddManagerRequest(@NotBlank @Email @Size(max = 320) String email) {
}
