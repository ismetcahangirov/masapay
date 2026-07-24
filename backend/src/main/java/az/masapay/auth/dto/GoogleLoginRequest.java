package az.masapay.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload from the frontend carrying the Google ID token (credential). */
public record GoogleLoginRequest(@NotBlank String idToken) {
}
