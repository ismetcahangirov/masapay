package az.masapay.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to exchange a refresh token for a new token pair. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
