package az.masapay.auth;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.auth.dto.GoogleLoginRequest;
import az.masapay.auth.dto.GoogleLoginResponse;
import az.masapay.auth.dto.RefreshRequest;
import az.masapay.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login and token refresh. The access token is returned in the body (the admin panel keeps it in
 * memory only); the refresh token is set as an httpOnly cookie and never appears in the body (#17).
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

	private final GoogleAuthService googleAuthService;
	private final TokenService tokenService;
	private final RefreshTokenCookie refreshCookie;

	public AuthController(GoogleAuthService googleAuthService, TokenService tokenService,
			RefreshTokenCookie refreshCookie) {
		this.googleAuthService = googleAuthService;
		this.tokenService = tokenService;
		this.refreshCookie = refreshCookie;
	}

	/**
	 * Verifies a Google ID token and provisions/updates the user. Enabled users
	 * receive an access token and the refresh cookie; pending users receive their profile only.
	 */
	@PostMapping("/google")
	public GoogleLoginResponse google(@Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
		User user = googleAuthService.authenticate(request.idToken());
		AuthTokens tokens = user.isEnabled() ? withCookie(tokenService.issueFor(user), response) : null;
		return GoogleLoginResponse.from(user, tokens);
	}

	/**
	 * Exchanges a valid refresh token for a new access token and a rotated refresh cookie. The
	 * token is read from the cookie; a JSON body {@code {"refreshToken": ...}} is still accepted
	 * for non-browser clients.
	 */
	@PostMapping("/refresh")
	public AuthTokens refresh(@CookieValue(name = RefreshTokenCookie.NAME, required = false) String cookieToken,
			@RequestBody(required = false) RefreshRequest request, HttpServletResponse response) {
		String refreshToken = cookieToken != null && !cookieToken.isBlank()
			? cookieToken
			: request == null ? null : request.refreshToken();
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BadCredentialsException("No refresh token");
		}
		return withCookie(tokenService.refresh(refreshToken), response);
	}

	/** Deletes the refresh cookie; the client drops its in-memory access token itself. */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.clear());
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ProblemDetail onBadCredentials(BadCredentialsException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	/** Moves the refresh token from the body into the httpOnly cookie. */
	private AuthTokens withCookie(AuthTokens tokens, HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(tokens.refreshToken()));
		return new AuthTokens(tokens.accessToken(), null, tokens.expiresInSeconds());
	}
}
