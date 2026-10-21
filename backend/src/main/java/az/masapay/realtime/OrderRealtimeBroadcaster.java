package az.masapay.realtime;

import az.masapay.order.CustomerOrderService;
import az.masapay.order.dto.TableOrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes a fresh order snapshot to the table's WebSocket subscribers. With Redis enabled the
 * snapshot goes through Redis so every backend instance relays it to its own clients; with Redis
 * disabled (single instance, e.g. local development) it is sent straight to the STOMP topic (#21).
 * Call this after any change to a table's order.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderRealtimeBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(OrderRealtimeBroadcaster.class);

	private final CustomerOrderService customerOrderService;
	private final StringRedisTemplate stringRedisTemplate;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;
	private final String orderChannel;

	public OrderRealtimeBroadcaster(CustomerOrderService customerOrderService,
			ObjectProvider<StringRedisTemplate> stringRedisTemplate, SimpMessagingTemplate messagingTemplate,
			ObjectMapper objectMapper, @Value("${masapay.redis.enabled:true}") boolean redisEnabled,
			@Value("${masapay.redis.order-channel:masapay:order-events}") String orderChannel) {
		this.customerOrderService = customerOrderService;
		this.stringRedisTemplate = redisEnabled ? stringRedisTemplate.getIfAvailable() : null;
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
		this.orderChannel = orderChannel;
	}

	/** Rebuilds the table's current order snapshot and broadcasts it to all instances. */
	public void broadcastOrderUpdate(UUID qrToken) {
		TableOrderResponse snapshot = customerOrderService.getCurrentOrder(qrToken);
		if (stringRedisTemplate == null) {
			messagingTemplate.convertAndSend(OrderRealtimeRelay.destination(qrToken), snapshot);
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(new OrderUpdateMessage(qrToken, snapshot));
			stringRedisTemplate.convertAndSend(orderChannel, payload);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize order update for qrToken {}", qrToken, e);
		}
	}
}
