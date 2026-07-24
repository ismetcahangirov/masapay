package az.masapay.auth;

import az.masapay.auth.dto.GoogleLoginRequest;
import az.masapay.auth.dto.GoogleLoginResponse;
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

	public AuthController(GoogleAuthService googleAuthService) {
		this.googleAuthService = googleAuthService;
	}

	/** Verifies a Google ID token, provisions/updates the user and returns their profile. */
	@PostMapping("/google")
	public GoogleLoginResponse google(@Valid @RequestBody GoogleLoginRequest request) {
		User user = googleAuthService.authenticate(request.idToken());
		return GoogleLoginResponse.from(user);
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ProblemDetail onBadCredentials(BadCredentialsException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}
}
