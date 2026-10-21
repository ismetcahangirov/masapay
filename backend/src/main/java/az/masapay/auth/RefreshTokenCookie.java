package az.masapay.auth;

import az.masapay.security.JwtService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh token lives in an httpOnly cookie (#17): page JavaScript cannot read it, so a script
 * injected into the admin panel cannot steal a long-lived login. It is only sent to /api/auth.
 */
@Component
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RefreshTokenCookie {

	public static final String NAME = "masapay_refresh";
	private static final String PATH = "/api/auth";

	private final Duration maxAge;
	private final boolean secure;

	public RefreshTokenCookie(JwtService jwtService, @Value("${masapay.auth.cookie-secure:true}") boolean secure) {
		this.maxAge = jwtService.getRefreshTokenTtl();
		this.secure = secure;
	}

	/** Set-Cookie value that stores the refresh token. */
	public String create(String refreshToken) {
		return build(refreshToken, maxAge);
	}

	/** Set-Cookie value that deletes the cookie (logout). */
	public String clear() {
		return build("", Duration.ZERO);
	}

	private String build(String value, Duration age) {
		return ResponseCookie.from(NAME, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Strict")
			.path(PATH)
			.maxAge(age)
			.build()
			.toString();
	}
}
