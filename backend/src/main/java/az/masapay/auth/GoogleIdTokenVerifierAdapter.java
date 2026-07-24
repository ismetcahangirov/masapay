package az.masapay.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Verifies Google ID tokens with the official Google client, which validates the
 * signature against Google's JWKS and checks the issuer, audience and expiry.
 */
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

	private final GoogleIdTokenVerifier verifier;

	public GoogleIdTokenVerifierAdapter(GoogleIdTokenVerifier verifier) {
		this.verifier = verifier;
	}

	@Override
	public GoogleUserInfo verify(String idToken) {
		if (idToken == null || idToken.isBlank()) {
			throw new BadCredentialsException("Google ID token is missing");
		}
		try {
			GoogleIdToken token = verifier.verify(idToken);
			if (token == null) {
				throw new BadCredentialsException("Invalid Google ID token");
			}
			GoogleIdToken.Payload payload = token.getPayload();
			return new GoogleUserInfo(
				payload.getSubject(),
				payload.getEmail(),
				(String) payload.get("name"),
				(String) payload.get("picture"));
		} catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
			// IllegalArgumentException covers a malformed (non-JWT) token string.
			throw new BadCredentialsException("Google ID token verification failed", e);
		}
	}
}
