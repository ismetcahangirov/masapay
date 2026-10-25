package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The Redis rate limiter against a real Redis (#158): two backend instances share one limit, and
 * every counter expires. Skipped when Docker is absent; CI runs it.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimiterIntegrationTest {

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	private static LettuceConnectionFactory connections;
	private static StringRedisTemplate redis;

	@BeforeAll
	static void connect() {
		connections = new LettuceConnectionFactory(
			new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
		connections.afterPropertiesSet();
		connections.start();
		redis = new StringRedisTemplate(connections);
	}

	@AfterAll
	static void disconnect() {
		connections.destroy();
	}

	@Test
	void twoInstancesShareOneLimit() {
		RedisRateLimiter first = new RedisRateLimiter(redis, Clock.systemUTC());
		RedisRateLimiter second = new RedisRateLimiter(redis, Clock.systemUTC());
		Duration hour = Duration.ofHours(1);

		for (int i = 0; i < 6; i++) {
			assertThat(first.tryAcquire("pay-min:shared", 10, hour)).isEmpty();
		}
		for (int i = 0; i < 4; i++) {
			assertThat(second.tryAcquire("pay-min:shared", 10, hour)).isEmpty();
		}
		// 6 + 4 = 10 allowed; the 11th, on either instance, is refused.
		OptionalLong eleventh = second.tryAcquire("pay-min:shared", 10, hour);
		assertThat(eleventh).isPresent();
		assertThat(first.tryAcquire("pay-min:shared", 10, hour)).isPresent();
	}

	@Test
	void everyCounterExpiresWithItsWindow() {
		new RedisRateLimiter(redis, Clock.systemUTC()).tryAcquire("auth:ttl", 5, Duration.ofMinutes(1));

		String key = redis.keys(RedisRateLimiter.PREFIX + "auth:ttl:*").iterator().next();
		Long ttl = redis.getExpire(key);
		assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(60);
	}
}
