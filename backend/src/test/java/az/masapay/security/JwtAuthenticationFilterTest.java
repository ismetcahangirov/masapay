package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

	private final JwtService jwtService = new JwtService("test-secret-0123456789-0123456789-abcdef",
			"masapay", Duration.ofMinutes(15), Duration.ofDays(7));
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
	private final User user = new User();

	JwtAuthenticationFilterTest() {
		user.setId(UUID.randomUUID());
		user.setEmail("manager@masapay.az");
		user.setRole(UserRole.RESTO_MANAGER);
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private Authentication run(String authorizationHeader) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (authorizationHeader != null) {
			request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request, new MockHttpServletResponse(), chain);
		assertThat(chain.getRequest()).as("the request always continues down the chain").isNotNull();
		return SecurityContextHolder.getContext().getAuthentication();
	}

	@Test
	void aValidAccessTokenAuthenticatesWithItsRole() throws Exception {
		Authentication auth = run("Bearer " + jwtService.generateAccessToken(user));

		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isEqualTo(
				new AuthenticatedUser(user.getId(), "manager@masapay.az", UserRole.RESTO_MANAGER));
		assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RESTO_MANAGER");
	}

	@Test
	void aRefreshTokenDoesNotAuthenticateApiCalls() throws Exception {
		assertThat(run("Bearer " + jwtService.generateRefreshToken(user))).isNull();
	}

	@Test
	void aBrokenTokenOrMissingHeaderLeavesTheRequestAnonymous() throws Exception {
		assertThat(run("Bearer not-a-jwt")).isNull();
		assertThat(run("Basic abc")).isNull();
		assertThat(run(null)).isNull();
	}
}
