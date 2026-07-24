package az.masapay.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.domain.User;
import az.masapay.repository.UserRepository;
import az.masapay.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private Claims claims;

	private TokenService tokenService;

	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		tokenService = new TokenService(jwtService, userRepository);
	}

	private User enabledUser() {
		User user = new User();
		user.setId(userId);
		user.setEmail("m@example.com");
		user.setEnabled(true);
		return user;
	}

	@Test
	void issueForReturnsBothTokens() {
		User user = enabledUser();
		when(jwtService.generateAccessToken(user)).thenReturn("access");
		when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
		when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));

		AuthTokens tokens = tokenService.issueFor(user);

		assertThat(tokens.accessToken()).isEqualTo("access");
		assertThat(tokens.refreshToken()).isEqualTo("refresh");
		assertThat(tokens.expiresInSeconds()).isEqualTo(900);
	}

	@Test
	void refreshWithValidTokenIssuesNewPair() {
		User user = enabledUser();
		when(jwtService.parse("r")).thenReturn(claims);
		when(jwtService.getType(claims)).thenReturn(JwtService.TYPE_REFRESH);
		when(jwtService.getUserId(claims)).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(jwtService.generateAccessToken(user)).thenReturn("access2");
		when(jwtService.generateRefreshToken(user)).thenReturn("refresh2");
		when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));

		AuthTokens tokens = tokenService.refresh("r");

		assertThat(tokens.accessToken()).isEqualTo("access2");
		assertThat(tokens.refreshToken()).isEqualTo("refresh2");
	}

	@Test
	void refreshRejectsAccessTokenType() {
		when(jwtService.parse("a")).thenReturn(claims);
		when(jwtService.getType(claims)).thenReturn(JwtService.TYPE_ACCESS);

		assertThatThrownBy(() -> tokenService.refresh("a")).isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void refreshRejectsExpiredToken() {
		when(jwtService.parse("r")).thenThrow(new ExpiredJwtException(null, null, "expired"));

		assertThatThrownBy(() -> tokenService.refresh("r")).isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void refreshRejectsDisabledUser() {
		User disabled = enabledUser();
		disabled.setEnabled(false);
		when(jwtService.parse("r")).thenReturn(claims);
		when(jwtService.getType(claims)).thenReturn(JwtService.TYPE_REFRESH);
		when(jwtService.getUserId(claims)).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(disabled));

		assertThatThrownBy(() -> tokenService.refresh("r")).isInstanceOf(BadCredentialsException.class);
	}
}
