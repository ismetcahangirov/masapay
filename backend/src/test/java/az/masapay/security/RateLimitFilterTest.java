package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.domain.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

	/** A clock the test moves forward by hand. */
	private static final class TestClock extends Clock {
		private Instant now = Instant.parse("2026-10-22T10:00:00Z");

		void advance(Duration by) {
			now = now.plus(by);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	}

	private final TestClock clock = new TestClock();
	private final RateLimitProperties limits = new RateLimitProperties(true, 2, 3, 2, 3, 4);
	private final RateLimitFilter filter = new RateLimitFilter(limits, new FixedWindowRateLimiter(clock),
			new ObjectMapper());

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	private int call(String method, String path, String ip) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setRemoteAddr(ip);
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}

	@Test
	void signInIsLimitedPerIpAndTheWindowResets() throws Exception {
		assertThat(call("POST", "/api/auth/google", "1.1.1.1")).isEqualTo(200);
		assertThat(call("POST", "/api/auth/google", "1.1.1.1")).isEqualTo(200);
		assertThat(call("POST", "/api/auth/google", "1.1.1.1")).isEqualTo(429);
		assertThat(call("POST", "/api/auth/google", "2.2.2.2")).isEqualTo(200);

		clock.advance(Duration.ofMinutes(1));
		assertThat(call("POST", "/api/auth/google", "1.1.1.1")).isEqualTo(200);
	}

	@Test
	void aRejectedRequestSaysWhenToRetry() throws Exception {
		call("POST", "/api/auth/refresh", "1.1.1.1");
		call("POST", "/api/auth/refresh", "1.1.1.1");
		clock.advance(Duration.ofSeconds(20));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
		request.setRemoteAddr("1.1.1.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("40");
		assertThat(response.getContentAsString()).contains("Too many requests");
	}

	@Test
	void startingPaymentsHasAMinuteAndADailyQuota() throws Exception {
		String checkout = "/api/public/tables/" + UUID.randomUUID() + "/payment/full/checkout";
		assertThat(call("POST", checkout, "3.3.3.3")).isEqualTo(200);
		assertThat(call("POST", checkout, "3.3.3.3")).isEqualTo(200);
		assertThat(call("POST", checkout, "3.3.3.3")).isEqualTo(429);

		clock.advance(Duration.ofMinutes(1));
		assertThat(call("POST", checkout, "3.3.3.3")).isEqualTo(200);
		clock.advance(Duration.ofMinutes(1));
		// Fourth start of the day: over the daily quota of 3 although the minute is fresh.
		assertThat(call("POST", checkout, "3.3.3.3")).isEqualTo(429);
	}

	@Test
	void customerReadsAreLimitedSeparatelyFromPaymentStarts() throws Exception {
		String bill = "/api/public/tables/" + UUID.randomUUID() + "/order";
		for (int i = 0; i < 3; i++) {
			assertThat(call("GET", bill, "4.4.4.4")).isEqualTo(200);
		}
		assertThat(call("GET", bill, "4.4.4.4")).isEqualTo(429);
	}

	@Test
	void payriffCallbacksAndNonApiPathsAreNeverLimited() throws Exception {
		for (int i = 0; i < 10; i++) {
			assertThat(call("POST", "/api/public/payriff/webhook", "5.5.5.5")).isEqualTo(200);
			assertThat(call("GET", "/actuator/health", "5.5.5.5")).isEqualTo(200);
		}
	}

	@Test
	void staffAreLimitedPerUserNotPerSharedIp() throws Exception {
		signIn(UUID.randomUUID());
		for (int i = 0; i < 4; i++) {
			assertThat(call("GET", "/api/admin/restaurants", "6.6.6.6")).isEqualTo(200);
		}
		assertThat(call("GET", "/api/admin/restaurants", "6.6.6.6")).isEqualTo(429);

		signIn(UUID.randomUUID());
		assertThat(call("GET", "/api/admin/restaurants", "6.6.6.6")).isEqualTo(200);
	}

	private void signIn(UUID userId) {
		AuthenticatedUser user = new AuthenticatedUser(userId, "m@x.az", UserRole.RESTO_MANAGER);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
	}
}
