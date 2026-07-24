package az.masapay.security;

import az.masapay.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates each request from a Bearer access token. An invalid, expired or
 * non-access token simply leaves the context unauthenticated, so the entry point
 * returns 401 for protected endpoints; it never rejects the request itself.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = extractToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticate(token, request);
		}
		chain.doFilter(request, response);
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			Claims claims = jwtService.parse(token);
			if (!JwtService.TYPE_ACCESS.equals(jwtService.getType(claims))) {
				return; // refresh tokens must not authenticate API calls
			}
			UserRole role = jwtService.getRole(claims);
			List<SimpleGrantedAuthority> authorities = role == null
				? List.of()
				: List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
			AuthenticatedUser principal =
				new AuthenticatedUser(jwtService.getUserId(claims), jwtService.getEmail(claims), role);

			var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException | IllegalArgumentException ex) {
			// Invalid/expired token: stay unauthenticated; the entry point handles the 401.
			SecurityContextHolder.clearContext();
		}
	}

	private String extractToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length()).trim();
		}
		return null;
	}
}
