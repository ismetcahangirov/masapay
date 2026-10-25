package az.masapay.security;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refuses to start with the signing secrets shipped in application.yml. Anyone can read those in
 * the repository: with the JWT default they could mint a SUPER_ADMIN token, with the webhook
 * default they could forge a "payment approved" callback. Local development opts in with
 * {@code MASAPAY_ALLOW_DEV_SECRETS=true}, which the dev-login flag also implies.
 * <p>
 * Outside development the startup also stops when the refresh cookie would travel over plain
 * HTTP, or when the dev login (sign in as anyone without Google) is on (#156).
 */
@Component
public class InsecureDefaultsGuard {

	/** Every default secret in application.yml contains this marker. */
	static final String DEFAULT_MARKER = "change-me";
	static final int MIN_SECRET_LENGTH = 32;

	private final String jwtSecret;
	private final String webhookSecret;
	private final boolean allowDevSecrets;
	private final boolean cookieSecure;
	private final boolean devLogin;

	@Autowired
	public InsecureDefaultsGuard(@Value("${masapay.jwt.secret}") String jwtSecret,
			@Value("${masapay.payriff.webhook-secret:}") String webhookSecret,
			@Value("${masapay.security.allow-dev-secrets:false}") boolean allowDevSecrets,
			@Value("${masapay.auth.cookie-secure:false}") boolean cookieSecure,
			@Value("${masapay.auth.dev-login.enabled:false}") boolean devLogin) {
		this.jwtSecret = jwtSecret;
		this.webhookSecret = webhookSecret;
		this.allowDevSecrets = allowDevSecrets;
		this.cookieSecure = cookieSecure;
		this.devLogin = devLogin;
	}

	/** Secrets only, with production-safe auth settings (as in the prod profile). */
	InsecureDefaultsGuard(String jwtSecret, String webhookSecret, boolean allowDevSecrets) {
		this(jwtSecret, webhookSecret, allowDevSecrets, true, false);
	}

	@PostConstruct
	public void check() {
		if (allowDevSecrets) {
			return;
		}
		List<String> problems = new ArrayList<>();
		problems.addAll(problems("MASAPAY_JWT_SECRET", jwtSecret));
		problems.addAll(problems("MASAPAY_PAYRIFF_WEBHOOK_SECRET", webhookSecret));
		if (!cookieSecure) {
			problems.add("MASAPAY_AUTH_COOKIE_SECURE is false (the refresh cookie would travel over plain HTTP)");
		}
		if (devLogin) {
			problems.add("MASAPAY_DEV_LOGIN_ENABLED is on (anyone could sign in without Google)");
		}
		if (!problems.isEmpty()) {
			throw new IllegalStateException("Refusing to start with insecure settings: " + String.join("; ", problems)
				+ ". Set real random values (e.g. `openssl rand -base64 48`), or MASAPAY_ALLOW_DEV_SECRETS=true"
				+ " for local development only.");
		}
	}

	static List<String> problems(String name, String value) {
		if (value == null || value.isBlank()) {
			return List.of(name + " is not set");
		}
		if (value.contains(DEFAULT_MARKER)) {
			return List.of(name + " is the public default from application.yml");
		}
		if (value.length() < MIN_SECRET_LENGTH) {
			return List.of(name + " is shorter than " + MIN_SECRET_LENGTH + " characters");
		}
		return List.of();
	}
}
