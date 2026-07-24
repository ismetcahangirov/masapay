package az.masapay.config;

import az.masapay.redis.RedisMessagePublisher;
import az.masapay.redis.RedisMessageSubscriber;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis wiring for the two roles Redis plays in masapay: a cache (Spring Cache
 * backed by Redis) and the pub/sub backbone for real-time fan-out (#21).
 * <p>
 * The connection itself is auto-configured by Spring Boot from
 * {@code spring.data.redis.*}. This config is gated on {@code masapay.redis.enabled}
 * (default true) so tests can run a full context without a Redis server.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "masapay.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

	private final String eventsChannel;

	public RedisConfig(@Value("${masapay.redis.events-channel:masapay:events}") String eventsChannel) {
		this.eventsChannel = eventsChannel;
	}

	@Bean
	public ChannelTopic eventsTopic() {
		return new ChannelTopic(eventsChannel);
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		StringRedisSerializer keySerializer = new StringRedisSerializer();
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
		template.setKeySerializer(keySerializer);
		template.setHashKeySerializer(keySerializer);
		template.setValueSerializer(valueSerializer);
		template.setHashValueSerializer(valueSerializer);
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(Duration.ofMinutes(10))
			.disableCachingNullValues()
			.serializeValuesWith(RedisSerializationContext.SerializationPair
				.fromSerializer(new GenericJackson2JsonRedisSerializer()));
		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(cacheConfig)
			.build();
	}

	@Bean
	public RedisMessageSubscriber redisMessageSubscriber() {
		return new RedisMessageSubscriber();
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(
			RedisConnectionFactory connectionFactory, RedisMessageSubscriber subscriber, ChannelTopic eventsTopic) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(subscriber, eventsTopic);
		return container;
	}

	@Bean
	public RedisMessagePublisher redisMessagePublisher(
			RedisTemplate<String, Object> redisTemplate, ChannelTopic eventsTopic) {
		return new RedisMessagePublisher(redisTemplate, eventsTopic);
	}
}
