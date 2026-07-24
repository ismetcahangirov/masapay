package az.masapay.auth.dto;

/** Access/refresh token pair returned on login and refresh. */
public record AuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {
}
