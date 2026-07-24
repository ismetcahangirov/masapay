package az.masapay.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

/**
 * Publishes messages to Redis pub/sub channels. Real-time consumers (the STOMP
 * relay in #21) subscribe to these channels to fan events out to clients.
 */
public class RedisMessagePublisher {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ChannelTopic defaultTopic;

	public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate, ChannelTopic defaultTopic) {
		this.redisTemplate = redisTemplate;
		this.defaultTopic = defaultTopic;
	}

	/** Publish to the default masapay events channel. */
	public void publish(Object message) {
		redisTemplate.convertAndSend(defaultTopic.getTopic(), message);
	}

	/** Publish to an explicit channel, e.g. a per-table topic. */
	public void publish(String channel, Object message) {
		redisTemplate.convertAndSend(channel, message);
	}
}
