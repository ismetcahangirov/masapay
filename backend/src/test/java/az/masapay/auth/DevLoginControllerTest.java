package az.masapay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.auth.dto.GoogleLoginResponse;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.UserRepository;
import az.masapay.security.JwtService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class DevLoginControllerTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final TokenService tokenService = mock(TokenService.class);
	private final RefreshTokenCookie cookie = new RefreshTokenCookie(
			new JwtService("test-secret-0123456789-0123456789-abcdef", "masapay", Duration.ofMinutes(15),
					Duration.ofDays(7)),
			false);
	private final DevLoginController controller = new DevLoginController(userRepository, tokenService, cookie);
	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	void isOffUnlessExplicitlyEnabled() {
		ConditionalOnProperty condition = DevLoginController.class.getAnnotation(ConditionalOnProperty.class);

		assertThat(condition.havingValue()).isEqualTo("true");
		assertThat(condition.matchIfMissing()).isFalse();
	}

	@Test
	void signsInAnEnabledUserWithTheRefreshCookie() {
		User admin = new User();
		admin.setId(UUID.randomUUID());
		admin.setEmail("admin@masapay.local");
		admin.setRole(UserRole.SUPER_ADMIN);
		admin.setEnabled(true);
		when(userRepository.findByEmail("admin@masapay.local")).thenReturn(Optional.of(admin));
		when(tokenService.issueFor(admin)).thenReturn(new AuthTokens("access", "refresh", 900));

		GoogleLoginResponse body = controller.devLogin(
				new DevLoginController.DevLoginRequest("admin@masapay.local"), response);

		assertThat(body.tokens().accessToken()).isEqualTo("access");
		assertThat(body.tokens().refreshToken()).isNull();
		assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).startsWith("masapay_refresh=refresh");
	}

	@Test
	void refusesUnknownOrDisabledUsers() {
		User pending = new User();
		pending.setEnabled(false);
		when(userRepository.findByEmail("pending@x.az")).thenReturn(Optional.of(pending));

		assertThatThrownBy(() -> controller.devLogin(new DevLoginController.DevLoginRequest("pending@x.az"), response))
				.isInstanceOf(BadCredentialsException.class);
		assertThatThrownBy(() -> controller.devLogin(new DevLoginController.DevLoginRequest("nobody@x.az"), response))
				.isInstanceOf(BadCredentialsException.class);
	}
}
