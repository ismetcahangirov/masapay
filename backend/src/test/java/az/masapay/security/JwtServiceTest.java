package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";
	private static final String ISSUER = "masapay-test";

	private JwtService service(Duration accessTtl) {
		return new JwtService(SECRET, ISSUER, accessTtl, Duration.ofDays(7));
	}

	private User enabledUser() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("manager@example.com");
		user.setRole(UserRole.RESTO_MANAGER);
		user.setEnabled(true);
		return user;
	}

	@Test
	void accessTokenCarriesIdentityClaims() {
		JwtService service = service(Duration.ofMinutes(15));
		User user = enabledUser();

		Claims claims = service.parse(service.generateAccessToken(user));

		assertThat(service.getUserId(claims)).isEqualTo(user.getId());
		assertThat(service.getEmail(claims)).isEqualTo("manager@example.com");
		assertThat(service.getRole(claims)).isEqualTo(UserRole.RESTO_MANAGER);
		assertThat(service.getType(claims)).isEqualTo(JwtService.TYPE_ACCESS);
		assertThat(claims.getIssuer()).isEqualTo(ISSUER);
	}

	@Test
	void refreshTokenIsTypedRefresh() {
		JwtService service = service(Duration.ofMinutes(15));

		Claims claims = service.parse(service.generateRefreshToken(enabledUser()));

		assertThat(service.getType(claims)).isEqualTo(JwtService.TYPE_REFRESH);
	}

	@Test
	void expiredTokenIsRejected() {
		JwtService service = service(Duration.ofSeconds(-10));
		String expired = service.generateAccessToken(enabledUser());

		assertThatThrownBy(() -> service.parse(expired)).isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void tamperedTokenIsRejected() {
		JwtService issuer = service(Duration.ofMinutes(15));
		JwtService otherSecret = new JwtService(
			"another-secret-another-secret-another-secret-xyz", ISSUER, Duration.ofMinutes(15), Duration.ofDays(7));
		String token = issuer.generateAccessToken(enabledUser());

		assertThatThrownBy(() -> otherSecret.parse(token)).isInstanceOf(io.jsonwebtoken.JwtException.class);
	}
}
