package az.masapay.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.order.CustomerOrderService;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.table.FloorStatus;
import az.masapay.table.dto.FloorTableView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class OrderRealtimeBroadcasterTest {

	private final CustomerOrderService customerOrderService = mock(CustomerOrderService.class);
	private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
	private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final UUID qrToken = UUID.randomUUID();
	private final TableOrderResponse snapshot =
			new TableOrderResponse(UUID.randomUUID(), "A1", UUID.randomUUID(), "Cafe", "AZN", null, null);

	@SuppressWarnings("unchecked")
	private OrderRealtimeBroadcaster broadcaster(boolean redisEnabled) {
		ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(stringRedisTemplate);
		when(customerOrderService.getCurrentOrder(qrToken)).thenReturn(snapshot);
		return new OrderRealtimeBroadcaster(customerOrderService, provider, messagingTemplate, objectMapper,
				redisEnabled, "masapay:order-events");
	}

	@Test
	void withRedisPublishesTheSnapshotToTheOrderChannel() throws Exception {
		broadcaster(true).broadcastOrderUpdate(qrToken);

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(stringRedisTemplate).convertAndSend(eq("masapay:order-events"), payload.capture());
		OrderUpdateMessage sent = objectMapper.readValue(payload.getValue(), OrderUpdateMessage.class);
		assertThat(sent.qrToken()).isEqualTo(qrToken);
		assertThat(sent.order().tableLabel()).isEqualTo("A1");
		// The relay on each instance sends the table and floor topics, not the broadcaster.
		verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
	}

	@Test
	void withoutRedisSendsStraightToTheTableTopic() {
		broadcaster(false).broadcastOrderUpdate(qrToken);

		verify(messagingTemplate).convertAndSend("/topic/tables/" + qrToken, snapshot);
		verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
	}

	@Test
	void withoutRedisAlsoSendsTheFloorEntryToTheRestaurantFloorTopic() {
		broadcaster(false).broadcastOrderUpdate(qrToken);

		ArgumentCaptor<Object> floor = ArgumentCaptor.forClass(Object.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/restaurants/" + snapshot.restaurantId() + "/floor"),
				floor.capture());
		assertThat(floor.getValue()).isInstanceOfSatisfying(FloorTableView.class, entry -> {
			assertThat(entry.tableId()).isEqualTo(snapshot.tableId());
			assertThat(entry.status()).isEqualTo(FloorStatus.EMPTY);
		});
	}
}
