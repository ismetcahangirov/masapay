package az.masapay.auth;

/** Verified identity claims extracted from a Google ID token. */
public record GoogleUserInfo(String subject, String email, String name, String pictureUrl) {
}
