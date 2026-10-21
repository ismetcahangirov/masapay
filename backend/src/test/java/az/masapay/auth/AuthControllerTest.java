package az.masapay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.auth.dto.GoogleLoginRequest;
import az.masapay.auth.dto.GoogleLoginResponse;
import az.masapay.auth.dto.RefreshRequest;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.security.JwtService;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class AuthControllerTest {

	private final GoogleAuthService googleAuthService = mock(GoogleAuthService.class);
	private final TokenService tokenService = mock(TokenService.class);
	private final RefreshTokenCookie cookie = new RefreshTokenCookie(
			new JwtService("test-secret-0123456789-0123456789-abcdef", "masapay", Duration.ofMinutes(15),
					Duration.ofDays(7)),
			true);
	private final AuthController controller = new AuthController(googleAuthService, tokenService, cookie);
	private final AuthTokens tokens = new AuthTokens("access", "refresh", 900);
	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private User user(boolean enabled) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("ali@masapay.az");
		user.setEnabled(enabled);
		user.setRole(enabled ? UserRole.WAITER : null);
		return user;
	}

	@Test
	void anEnabledUserGetsAnAccessTokenAndAnHttpOnlyRefreshCookie() {
		User user = user(true);
		when(googleAuthService.authenticate("id-token")).thenReturn(user);
		when(tokenService.issueFor(user)).thenReturn(tokens);

		GoogleLoginResponse body = controller.google(new GoogleLoginRequest("id-token"), response);

		assertThat(body.tokens().accessToken()).isEqualTo("access");
		assertThat(body.tokens().refreshToken()).as("never in the body").isNull();
		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).startsWith("masapay_refresh=refresh")
				.contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth");
	}

	@Test
	void aPendingUserGetsTheirProfileButNoTokens() {
		when(googleAuthService.authenticate("id-token")).thenReturn(user(false));

		GoogleLoginResponse body = controller.google(new GoogleLoginRequest("id-token"), response);

		assertThat(body.enabled()).isFalse();
		assertThat(body.tokens()).isNull();
		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
		verifyNoInteractions(tokenService);
	}

	@Test
	void refreshReadsTheCookieAndRotatesIt() {
		when(tokenService.refresh("old-refresh")).thenReturn(tokens);

		AuthTokens body = controller.refresh("old-refresh", null, response);

		assertThat(body.accessToken()).isEqualTo("access");
		assertThat(body.refreshToken()).isNull();
		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).startsWith("masapay_refresh=refresh");
	}

	@Test
	void refreshStillAcceptsABodyForNonBrowserClients() {
		when(tokenService.refresh("body-refresh")).thenReturn(tokens);

		assertThat(controller.refresh(null, new RefreshRequest("body-refresh"), response).accessToken())
				.isEqualTo("access");
	}

	@Test
	void refreshWithoutAnyTokenIsRejected() {
		assertThatThrownBy(() -> controller.refresh(null, null, response))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void logoutDeletesTheCookie() {
		controller.logout(response);

		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).startsWith("masapay_refresh=;")
				.contains("Max-Age=0");
	}

	@Test
	void badCredentialsBecome401ProblemDetails() {
		ProblemDetail problem = controller.onBadCredentials(new BadCredentialsException("Invalid Google token"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(problem.getDetail()).isEqualTo("Invalid Google token");
	}
}
