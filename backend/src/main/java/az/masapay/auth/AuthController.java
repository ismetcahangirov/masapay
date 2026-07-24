package az.masapay.auth;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.auth.dto.GoogleLoginRequest;
import az.masapay.auth.dto.GoogleLoginResponse;
import az.masapay.auth.dto.RefreshRequest;
import az.masapay.domain.User;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

	private final GoogleAuthService googleAuthService;
	private final TokenService tokenService;

	public AuthController(GoogleAuthService googleAuthService, TokenService tokenService) {
		this.googleAuthService = googleAuthService;
		this.tokenService = tokenService;
	}

	/**
	 * Verifies a Google ID token and provisions/updates the user. Enabled users
	 * receive an access/refresh token pair; pending users receive their profile only.
	 */
	@PostMapping("/google")
	public GoogleLoginResponse google(@Valid @RequestBody GoogleLoginRequest request) {
		User user = googleAuthService.authenticate(request.idToken());
		AuthTokens tokens = user.isEnabled() ? tokenService.issueFor(user) : null;
		return GoogleLoginResponse.from(user, tokens);
	}

	/** Exchanges a valid refresh token for a new token pair. */
	@PostMapping("/refresh")
	public AuthTokens refresh(@Valid @RequestBody RefreshRequest request) {
		return tokenService.refresh(request.refreshToken());
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ProblemDetail onBadCredentials(BadCredentialsException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}
}
