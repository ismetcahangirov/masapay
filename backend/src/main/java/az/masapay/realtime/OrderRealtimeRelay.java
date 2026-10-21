package az.masapay.realtime;

import az.masapay.order.dto.TableOrderResponse;
import az.masapay.table.dto.FloorTableView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Receives order-update messages from Redis and forwards them to the local STOMP
 * broker, so clients connected to this instance and subscribed to the table topic
 * are updated, and so is the restaurant's live floor plan (#44). Running on every instance is what makes real-time work when clients
 * are spread across a horizontally scaled backend.
 */
@Component
@ConditionalOnProperty(prefix = "masapay.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderRealtimeRelay implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(OrderRealtimeRelay.class);

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	public OrderRealtimeRelay(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
	}

	/** STOMP topic a table's customers subscribe to. */
	static String destination(UUID qrToken) {
		return "/topic/tables/" + qrToken;
	}

	/** STOMP topic a restaurant's admin live floor plan subscribes to (#44). */
	static String floorDestination(UUID restaurantId) {
		return "/topic/restaurants/" + restaurantId + "/floor";
	}

	/** Sends the snapshot to the table's customers and the table's floor entry to its restaurant's floor. */
	static void deliver(SimpMessagingTemplate messagingTemplate, UUID qrToken, TableOrderResponse snapshot) {
		messagingTemplate.convertAndSend(destination(qrToken), snapshot);
		messagingTemplate.convertAndSend(floorDestination(snapshot.restaurantId()),
			FloorTableView.of(snapshot, Instant.now()));
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			String body = new String(message.getBody(), StandardCharsets.UTF_8);
			OrderUpdateMessage update = objectMapper.readValue(body, OrderUpdateMessage.class);
			deliver(messagingTemplate, update.qrToken(), update.order());
		} catch (Exception e) {
			log.error("Failed to relay order update from Redis to STOMP", e);
		}
	}
}
