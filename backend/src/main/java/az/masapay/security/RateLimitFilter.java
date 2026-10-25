package az.masapay.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers 429 when one client sends too many requests: sign-in and the customer (QR) endpoints
 * per IP, starting a payment per IP per minute and per day (each start creates a Payriff order),
 * and staff per user. Runs after the JWT filter so a signed-in user is known. Payriff callbacks
 * are left alone: they are signed, and a retrying Payriff must not be locked out.
 *
 * <p>The IP is {@link HttpServletRequest#getRemoteAddr()}, never a raw X-Forwarded-For header a
 * client could fake. Behind a reverse proxy, Spring's forward-headers support fills it in.
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Duration MINUTE = Duration.ofMinutes(1);
	private static final Duration DAY = Duration.ofDays(1);
	private static final Pattern PAYMENT_START = Pattern.compile("^/api/public/tables/[^/]+/payment/.+/checkout$");

	private final RateLimitProperties limits;
	private final RateLimiter limiter;
	private final ObjectMapper objectMapper;

	public RateLimitFilter(RateLimitProperties limits, RateLimiter limiter, ObjectMapper objectMapper) {
		this.limits = limits;
		this.limiter = limiter;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		OptionalLong retryAfter = check(request);
		if (retryAfter.isPresent()) {
			reject(request, response, retryAfter.getAsLong());
			return;
		}
		chain.doFilter(request, response);
	}

	private OptionalLong check(HttpServletRequest request) {
		String path = request.getRequestURI();
		String ip = request.getRemoteAddr();
		if (!path.startsWith("/api/") || path.startsWith("/api/public/payriff/")) {
			return OptionalLong.empty();
		}
		if (path.startsWith("/api/auth/")) {
			return limiter.tryAcquire("auth:" + ip, limits.authPerMinute(), MINUTE);
		}
		if ("POST".equals(request.getMethod()) && PAYMENT_START.matcher(path).matches()) {
			OptionalLong perMinute = limiter.tryAcquire("pay-min:" + ip, limits.paymentPerMinute(), MINUTE);
			return perMinute.isPresent()
				? perMinute
				: limiter.tryAcquire("pay-day:" + ip, limits.paymentPerDay(), DAY);
		}
		if (path.startsWith("/api/public/")) {
			return limiter.tryAcquire("public:" + ip, limits.publicPerMinute(), MINUTE);
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
			return limiter.tryAcquire("staff:" + user.id(), limits.staffPerMinute(), MINUTE);
		}
		// Not signed in: the request will get 401, but guessing tokens still costs a slot.
		return limiter.tryAcquire("anon:" + ip, limits.publicPerMinute(), MINUTE);
	}

	private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
			throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
			"Too many requests, try again in " + retryAfterSeconds + " s");
		problem.setInstance(URI.create(request.getRequestURI()));
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), problem);
	}
}
