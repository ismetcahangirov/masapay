package az.masapay.security;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Rate limit counters in Redis (#158), so every backend instance counts against the same limit
 * and a restart does not reset them (the daily payment quota above all).
 * <p>
 * Windows are aligned to the clock: the key is {@code rl:<bucket>:<window number>}, where the
 * window number is the time divided by the window length, so all instances agree on it without
 * talking to each other. One Lua script increments the counter and, on the first request of a
 * window, sets its expiry, atomically: no counter is ever left without one.
 * <p>
 * If Redis can not be reached the request is let through (fail-open) and a warning is logged at
 * most once a minute: a limiter outage must not take the API down with it.
 */
public class RedisRateLimiter implements RateLimiter {

	private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

	static final String PREFIX = "rl:";
	private static final long WARN_EVERY_MILLIS = 60_000;

	@SuppressWarnings("rawtypes")
	private static final DefaultRedisScript<List> INCREMENT = new DefaultRedisScript<>("""
		local count = redis.call('INCR', KEYS[1])
		if count == 1 then
			redis.call('PEXPIRE', KEYS[1], ARGV[1])
		end
		return {count}
		""", List.class);

	private final StringRedisTemplate redis;
	private final Clock clock;
	private final AtomicLong lastWarning = new AtomicLong(Long.MIN_VALUE / 2);

	public RedisRateLimiter(StringRedisTemplate redis, Clock clock) {
		this.redis = redis;
		this.clock = clock;
	}

	@Override
	public OptionalLong tryAcquire(String key, int limit, Duration window) {
		long now = clock.millis();
		long length = window.toMillis();
		long index = now / length;
		long leftMillis = (index + 1) * length - now;
		long count;
		try {
			List<?> result = redis.execute(INCREMENT, List.of(PREFIX + key + ":" + index),
				Long.toString(leftMillis));
			count = ((Number) result.get(0)).longValue();
		}
		catch (RuntimeException e) {
			warnAtMostOnceAMinute(now, e);
			return OptionalLong.empty();
		}
		if (count <= limit) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(Math.max(1, (leftMillis + 999) / 1000));
	}

	private void warnAtMostOnceAMinute(long now, RuntimeException e) {
		long last = lastWarning.get();
		if (now - last >= WARN_EVERY_MILLIS && lastWarning.compareAndSet(last, now)) {
			log.warn("Rate limiting is off: Redis can not be reached ({}). Requests are let through.",
				e.getMessage());
		}
	}
}
