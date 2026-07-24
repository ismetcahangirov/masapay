package az.masapay.security;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates HMAC-signed JWTs. Access tokens carry the user's identity
 * and role for stateless authorization; refresh tokens are exchanged for a new
 * pair. Tokens are distinguished by a {@code type} claim. Has no infrastructure
 * dependencies so it is used by the request filter without a database hit.
 */
@Service
public class JwtService {

	public static final String TYPE_ACCESS = "access";
	public static final String TYPE_REFRESH = "refresh";

	private static final String CLAIM_TYPE = "type";
	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_ROLE = "role";

	private final SecretKey key;
	private final String issuer;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;

	public JwtService(
			@Value("${masapay.jwt.secret}") String secret,
			@Value("${masapay.jwt.issuer}") String issuer,
			@Value("${masapay.jwt.access-token-ttl}") Duration accessTokenTtl,
			@Value("${masapay.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.issuer = issuer;
		this.accessTokenTtl = accessTokenTtl;
		this.refreshTokenTtl = refreshTokenTtl;
	}

	public String generateAccessToken(User user) {
		return build(user, TYPE_ACCESS, accessTokenTtl);
	}

	public String generateRefreshToken(User user) {
		return build(user, TYPE_REFRESH, refreshTokenTtl);
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	private String build(User user, String type, Duration ttl) {
		Instant now = Instant.now();
		var builder = Jwts.builder()
			.subject(user.getId().toString())
			.issuer(issuer)
			.claim(CLAIM_TYPE, type)
			.claim(CLAIM_EMAIL, user.getEmail())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(ttl)))
			.signWith(key);
		if (user.getRole() != null) {
			builder.claim(CLAIM_ROLE, user.getRole().name());
		}
		return builder.compact();
	}

	/**
	 * Parses and verifies a token's signature, issuer and expiry.
	 *
	 * @throws io.jsonwebtoken.JwtException if the token is malformed, tampered or expired
	 */
	public Claims parse(String token) {
		Jws<Claims> jws = Jwts.parser()
			.verifyWith(key)
			.requireIssuer(issuer)
			.build()
			.parseSignedClaims(token);
		return jws.getPayload();
	}

	public UUID getUserId(Claims claims) {
		return UUID.fromString(claims.getSubject());
	}

	public String getType(Claims claims) {
		return claims.get(CLAIM_TYPE, String.class);
	}

	public String getEmail(Claims claims) {
		return claims.get(CLAIM_EMAIL, String.class);
	}

	public UserRole getRole(Claims claims) {
		String role = claims.get(CLAIM_ROLE, String.class);
		return role == null ? null : UserRole.valueOf(role);
	}
}
