package az.masapay.config;

import az.masapay.auth.GoogleIdTokenVerifierAdapter;
import az.masapay.auth.GoogleTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the Google ID token verifier from the configured OAuth2 client id, which
 * is used as the expected token audience. The Google client id comes from
 * {@code masapay.auth.google.client-id} (env-supplied secret).
 */
@Configuration
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthConfig {

	@Bean
	public GoogleTokenVerifier googleTokenVerifier(
			@Value("${masapay.auth.google.client-id:}") String clientId) {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
				new NetHttpTransport(), GsonFactory.getDefaultInstance())
			.setAudience(Collections.singletonList(clientId))
			.build();
		return new GoogleIdTokenVerifierAdapter(verifier);
	}
}
