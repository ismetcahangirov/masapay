package az.masapay.auth;

import az.masapay.auth.dto.AuthTokens;
import az.masapay.domain.User;
import az.masapay.repository.UserRepository;
import az.masapay.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * Issues token pairs for authenticated users and exchanges refresh tokens for new
 * pairs. Refresh re-checks the user still exists and is enabled, and re-reads the
 * current role, so a deactivated or re-roled account cannot keep a live session.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TokenService {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public TokenService(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	public AuthTokens issueFor(User user) {
		String access = jwtService.generateAccessToken(user);
		String refresh = jwtService.generateRefreshToken(user);
		return new AuthTokens(access, refresh, jwtService.getAccessTokenTtl().toSeconds());
	}

	public AuthTokens refresh(String refreshToken) {
		Claims claims;
		try {
			claims = jwtService.parse(refreshToken);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new BadCredentialsException("Invalid or expired refresh token", ex);
		}
		if (!JwtService.TYPE_REFRESH.equals(jwtService.getType(claims))) {
			throw new BadCredentialsException("Provided token is not a refresh token");
		}

		UUID userId = jwtService.getUserId(claims);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadCredentialsException("User no longer exists"));
		if (!user.isEnabled()) {
			throw new BadCredentialsException("User is not active");
		}
		return issueFor(user);
	}
}
