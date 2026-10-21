package az.masapay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.auth.dto.GoogleLoginRequest;
import az.masapay.auth.dto.GoogleLoginResponse;
import az.masapay.auth.dto.RefreshRequest;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;

class AuthControllerTest {

	private final GoogleAuthService googleAuthService = mock(GoogleAuthService.class);
	private final TokenService tokenService = mock(TokenService.class);
	private final AuthController controller = new AuthController(googleAuthService, tokenService);
	private final AuthTokens tokens = new AuthTokens("access", "refresh", 900);

	private User user(boolean enabled) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("ali@masapay.az");
		user.setEnabled(enabled);
		user.setRole(enabled ? UserRole.WAITER : null);
		return user;
	}

	@Test
	void anEnabledUserGetsATokenPair() {
		User user = user(true);
		when(googleAuthService.authenticate("id-token")).thenReturn(user);
		when(tokenService.issueFor(user)).thenReturn(tokens);

		GoogleLoginResponse response = controller.google(new GoogleLoginRequest("id-token"));

		assertThat(response.enabled()).isTrue();
		assertThat(response.tokens()).isEqualTo(tokens);
		assertThat(response.email()).isEqualTo("ali@masapay.az");
	}

	@Test
	void aPendingUserGetsTheirProfileButNoTokens() {
		when(googleAuthService.authenticate("id-token")).thenReturn(user(false));

		GoogleLoginResponse response = controller.google(new GoogleLoginRequest("id-token"));

		assertThat(response.enabled()).isFalse();
		assertThat(response.tokens()).isNull();
		verifyNoInteractions(tokenService);
	}

	@Test
	void refreshExchangesTheRefreshToken() {
		when(tokenService.refresh("refresh")).thenReturn(tokens);

		assertThat(controller.refresh(new RefreshRequest("refresh"))).isEqualTo(tokens);
	}

	@Test
	void badCredentialsBecome401ProblemDetails() {
		ProblemDetail problem = controller.onBadCredentials(new BadCredentialsException("Invalid Google token"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(problem.getDetail()).isEqualTo("Invalid Google token");
	}
}
