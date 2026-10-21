package az.masapay.auth;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.auth.dto.GoogleLoginResponse;
import az.masapay.domain.User;
import az.masapay.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * LOCAL DEVELOPMENT ONLY: signs in as an existing, enabled user by email, without Google, so the
 * admin panel can be tried before a Google OAuth client exists. The bean is only created when
 * {@code masapay.auth.dev-login.enabled=true} (default false), so the endpoint does not exist in a
 * normal deployment. It issues the same tokens and refresh cookie as the Google login.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "masapay.auth.dev-login", name = "enabled", havingValue = "true")
public class DevLoginController {

	private static final Logger log = LoggerFactory.getLogger(DevLoginController.class);

	private final UserRepository userRepository;
	private final TokenService tokenService;
	private final RefreshTokenCookie refreshCookie;

	public DevLoginController(UserRepository userRepository, TokenService tokenService,
			RefreshTokenCookie refreshCookie) {
		this.userRepository = userRepository;
		this.tokenService = tokenService;
		this.refreshCookie = refreshCookie;
	}

	@PostConstruct
	void warn() {
		log.warn("DEV LOGIN IS ENABLED: anyone can sign in as any enabled user. Local development only.");
	}

	@PostMapping("/dev-login")
	public GoogleLoginResponse devLogin(@Valid @RequestBody DevLoginRequest request, HttpServletResponse response) {
		User user = userRepository.findByEmail(request.email().trim())
			.filter(User::isEnabled)
			.orElseThrow(() -> new BadCredentialsException("No enabled user with this email"));
		AuthTokens tokens = tokenService.issueFor(user);
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(tokens.refreshToken()));
		return GoogleLoginResponse.from(user,
			new AuthTokens(tokens.accessToken(), null, tokens.expiresInSeconds()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ProblemDetail onBadCredentials(BadCredentialsException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	/** Email of a seeded or existing user to sign in as. */
	public record DevLoginRequest(@NotBlank @Email String email) {}
}
