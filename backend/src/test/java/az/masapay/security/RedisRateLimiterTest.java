package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRateLimiterTest {

	private static final Duration MINUTE = Duration.ofMinutes(1);

	private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
	/** 20 seconds into a minute window. */
	private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L * 1000 + 20_000), ZoneOffset.UTC);
	private final RedisRateLimiter limiter = new RedisRateLimiter(redis, clock);

	@SuppressWarnings("unchecked")
	private void redisCounts(long count) {
		when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(List.of(count));
	}

	@Test
	@SuppressWarnings("unchecked")
	void countsInAClockAlignedWindowSharedByEveryInstance() {
		redisCounts(10);

		assertThat(limiter.tryAcquire("pay-min:1.2.3.4", 10, MINUTE)).isEmpty();
		// Window number 1000 of one minute; it expires when the window ends, 40 s from now.
		verify(redis).execute(any(RedisScript.class), eq(List.of("rl:pay-min:1.2.3.4:1000")), eq("40000"));
	}

	@Test
	void overTheLimitSaysWhenToTryAgain() {
		redisCounts(11);

		assertThat(limiter.tryAcquire("pay-min:1.2.3.4", 10, MINUTE)).isEqualTo(OptionalLong.of(40));
	}

	@Test
	@SuppressWarnings("unchecked")
	void withRedisDownRequestsAreLetThrough() {
		when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
			.thenThrow(new RedisConnectionFailureException("Connection refused"));

		assertThat(limiter.tryAcquire("auth:1.2.3.4", 1, MINUTE)).isEmpty();
		assertThat(limiter.tryAcquire("auth:1.2.3.4", 1, MINUTE)).isEmpty();
	}
}
