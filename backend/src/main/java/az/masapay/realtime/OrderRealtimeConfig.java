package az.masapay.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** Subscribes the order relay to the Redis order-events channel. */
@Configuration
@ConditionalOnProperty(prefix = "masapay.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderRealtimeConfig {

	public OrderRealtimeConfig(RedisMessageListenerContainer container, OrderRealtimeRelay relay,
			@Value("${masapay.redis.order-channel:masapay:order-events}") String orderChannel) {
		container.addMessageListener(relay, new ChannelTopic(orderChannel));
	}
}
