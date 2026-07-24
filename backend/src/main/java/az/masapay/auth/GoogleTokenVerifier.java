package az.masapay.auth;

/**
 * Verifies a Google ID token and returns its identity claims. Abstracted from
 * the concrete Google client so the authentication logic can be unit-tested.
 */
public interface GoogleTokenVerifier {

	/**
	 * @param idToken the Google ID token supplied by the client
	 * @return the verified user info
	 * @throws org.springframework.security.authentication.BadCredentialsException if the token is missing, invalid or expired
	 */
	GoogleUserInfo verify(String idToken);
}
