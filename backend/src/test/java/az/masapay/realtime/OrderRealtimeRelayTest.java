package az.masapay.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class OrderRealtimeRelayTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void relaysRedisMessageToTableTopic() throws Exception {
		UUID qrToken = UUID.randomUUID();
		OrderView order = new OrderView(
			UUID.randomUUID(), "OPEN", new BigDecimal("7.00"), Instant.parse("2026-01-01T00:00:00Z"), List.of());
		TableOrderResponse snapshot =
			new TableOrderResponse(UUID.randomUUID(), "A1", UUID.randomUUID(), "Cafe", "AZN", order);
		byte[] body = objectMapper.writeValueAsString(new OrderUpdateMessage(qrToken, snapshot))
			.getBytes(StandardCharsets.UTF_8);

		OrderRealtimeRelay relay = new OrderRealtimeRelay(messagingTemplate, objectMapper);
		relay.onMessage(new DefaultMessage("masapay:order-events".getBytes(StandardCharsets.UTF_8), body), null);

		ArgumentCaptor<String> destination = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
		verify(messagingTemplate).convertAndSend(destination.capture(), payload.capture());
		assertThat(destination.getValue()).isEqualTo("/topic/tables/" + qrToken);
		assertThat(payload.getValue()).isInstanceOf(TableOrderResponse.class);
		assertThat(((TableOrderResponse) payload.getValue()).order().status()).isEqualTo("OPEN");
	}
}
