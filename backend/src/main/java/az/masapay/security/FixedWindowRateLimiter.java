package az.masapay.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts requests per key in fixed windows (e.g. one minute) kept in memory. Good enough for one
 * instance and for local runs without Redis; with Redis on, {@link RedisRateLimiter} is used so
 * several instances share the counters (#158).
 */
public class FixedWindowRateLimiter implements RateLimiter {

	/** Above this many keys, expired windows are dropped so memory stays bounded. */
	static final int CLEANUP_THRESHOLD = 50_000;

	private record Window(long startMillis, long lengthMillis, int count) {

		boolean expired(long now) {
			return now >= startMillis + lengthMillis;
		}
	}

	private final Map<String, Window> windows = new ConcurrentHashMap<>();
	private final Clock clock;

	public FixedWindowRateLimiter(Clock clock) {
		this.clock = clock;
	}

	@Override
	public OptionalLong tryAcquire(String key, int limit, Duration window) {
		long now = clock.millis();
		long length = window.toMillis();
		Window current = windows.compute(key, (k, existing) -> existing == null || existing.expired(now)
			? new Window(now, length, 1)
			: new Window(existing.startMillis(), length, existing.count() + 1));
		if (windows.size() > CLEANUP_THRESHOLD) {
			windows.values().removeIf(w -> w.expired(now));
		}
		if (current.count() <= limit) {
			return OptionalLong.empty();
		}
		long waitMillis = current.startMillis() + length - now;
		return OptionalLong.of(Math.max(1, (waitMillis + 999) / 1000));
	}

	int size() {
		return windows.size();
	}
}
