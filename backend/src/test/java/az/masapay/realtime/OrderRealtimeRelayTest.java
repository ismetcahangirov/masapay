package az.masapay.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import az.masapay.order.dto.OrderItemView;
import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.table.FloorStatus;
import az.masapay.table.dto.FloorTableView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
		OrderItemView byWaiter = new OrderItemView(UUID.randomUUID(), "Çay", BigDecimal.ONE, new BigDecimal("7.00"),
			new BigDecimal("7.00"), false, null, OrderItemView.Source.STAFF, "Aysel",
			Instant.parse("2026-01-01T00:00:00Z"));
		OrderView order = new OrderView(
			UUID.randomUUID(), "OPEN", new BigDecimal("7.00"), BigDecimal.ZERO, new BigDecimal("7.00"), Instant.parse("2026-01-01T00:00:00Z"), List.of(byWaiter), null, null, null);
		TableOrderResponse snapshot =
			new TableOrderResponse(UUID.randomUUID(), "A1", UUID.randomUUID(), "Cafe", "AZN", null, order);
		byte[] body = objectMapper.writeValueAsString(new OrderUpdateMessage(qrToken, snapshot))
			.getBytes(StandardCharsets.UTF_8);

		OrderRealtimeRelay relay = new OrderRealtimeRelay(messagingTemplate, objectMapper);
		relay.onMessage(new DefaultMessage("masapay:order-events".getBytes(StandardCharsets.UTF_8), body), null);

		ArgumentCaptor<String> destination = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
		verify(messagingTemplate, times(3)).convertAndSend(destination.capture(), payload.capture());
		assertThat(destination.getAllValues().get(0)).isEqualTo("/topic/tables/" + qrToken);
		assertThat(payload.getAllValues().get(0)).isInstanceOf(TableOrderResponse.class);
		assertThat(((TableOrderResponse) payload.getAllValues().get(0)).order().status()).isEqualTo("OPEN");
		// The table topic is public: the waiter's name stays out of it (#144).
		assertThat(((TableOrderResponse) payload.getAllValues().get(0)).order().items().get(0).addedBy()).isNull();

		// The same message also updates the restaurant's live floor plan (#44).
		assertThat(destination.getAllValues().get(1))
			.isEqualTo("/topic/restaurants/" + snapshot.restaurantId() + "/floor");
		assertThat(payload.getAllValues().get(1)).isInstanceOfSatisfying(FloorTableView.class, entry -> {
			assertThat(entry.tableId()).isEqualTo(snapshot.tableId());
			assertThat(entry.status()).isEqualTo(FloorStatus.ACTIVE);
			assertThat(entry.outstanding()).isEqualByComparingTo("7.00");
		});

		// ... and pings the kitchen screens to reload (#142).
		assertThat(destination.getAllValues().get(2))
			.isEqualTo("/topic/restaurants/" + snapshot.restaurantId() + "/kitchen");
		assertThat(payload.getAllValues().get(2)).isEqualTo(Map.of("tableId", snapshot.tableId()));
	}
}
