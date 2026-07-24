package az.masapay.realtime;

import az.masapay.order.CustomerOrderService;
import az.masapay.order.dto.TableOrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes a fresh order snapshot to Redis so every backend instance can push it
 * to its connected WebSocket clients. Call this after any change to a table's
 * order (e.g. a new item synced from the POS in #40).
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderRealtimeBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(OrderRealtimeBroadcaster.class);

	private final CustomerOrderService customerOrderService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String orderChannel;

	public OrderRealtimeBroadcaster(CustomerOrderService customerOrderService,
			StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
			@Value("${masapay.redis.order-channel:masapay:order-events}") String orderChannel) {
		this.customerOrderService = customerOrderService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.orderChannel = orderChannel;
	}

	/** Rebuilds the table's current order snapshot and broadcasts it to all instances. */
	public void broadcastOrderUpdate(UUID qrToken) {
		TableOrderResponse snapshot = customerOrderService.getCurrentOrder(qrToken);
		try {
			String payload = objectMapper.writeValueAsString(new OrderUpdateMessage(qrToken, snapshot));
			stringRedisTemplate.convertAndSend(orderChannel, payload);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize order update for qrToken {}", qrToken, e);
		}
	}
}
