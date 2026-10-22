package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InsecureDefaultsGuardTest {

	private static final String SHIPPED_JWT = "masapay-dev-secret-change-me-0123456789-abcdefghij";
	private static final String REAL = "q8Jv3m0yX1cR7tLw9ZpB4nE6sKdH2aUf5gVi0oMx";

	@Test
	void theShippedDefaultSecretsStopTheStartup() {
		InsecureDefaultsGuard guard = new InsecureDefaultsGuard(SHIPPED_JWT, REAL, false);

		assertThatThrownBy(guard::check)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("MASAPAY_JWT_SECRET is the public default");
	}

	@Test
	void aMissingOrShortWebhookSecretStopsTheStartup() {
		assertThatThrownBy(new InsecureDefaultsGuard(REAL, "", false)::check)
			.hasMessageContaining("MASAPAY_PAYRIFF_WEBHOOK_SECRET is not set");
		assertThatThrownBy(new InsecureDefaultsGuard(REAL, "short", false)::check)
			.hasMessageContaining("shorter than 32");
	}

	@Test
	void realSecretsStart() {
		assertThatCode(new InsecureDefaultsGuard(REAL, REAL + "x", false)::check).doesNotThrowAnyException();
	}

	@Test
	void localDevelopmentMayOptIn() {
		assertThatCode(new InsecureDefaultsGuard(SHIPPED_JWT, "", true)::check).doesNotThrowAnyException();
	}
}
