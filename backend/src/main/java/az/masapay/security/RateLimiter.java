package az.masapay.security;

import java.time.Duration;
import java.util.OptionalLong;

/**
 * Counts requests per key in fixed windows (#155). In memory for a single instance, in Redis when
 * several instances must share the counters (#158).
 */
public interface RateLimiter {

	/**
	 * Counts one request for {@code key}. Empty when it is allowed; otherwise the seconds until the
	 * window resets, for a Retry-After header.
	 */
	OptionalLong tryAcquire(String key, int limit, Duration window);
}
