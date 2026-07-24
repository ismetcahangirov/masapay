package az.masapay.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.order.CustomerOrderService;
import az.masapay.order.dto.TableOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class OrderRealtimeBroadcasterTest {

	@Mock
	private CustomerOrderService customerOrderService;
	@Mock
	private StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void publishesSnapshotToOrderChannel() throws Exception {
		UUID qrToken = UUID.randomUUID();
		TableOrderResponse snapshot =
			new TableOrderResponse(UUID.randomUUID(), "A1", UUID.randomUUID(), "Cafe", "AZN", null);
		when(customerOrderService.getCurrentOrder(qrToken)).thenReturn(snapshot);

		OrderRealtimeBroadcaster broadcaster =
			new OrderRealtimeBroadcaster(customerOrderService, stringRedisTemplate, objectMapper, "masapay:order-events");
		broadcaster.broadcastOrderUpdate(qrToken);

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(stringRedisTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("masapay:order-events"), payload.capture());

		OrderUpdateMessage sent = objectMapper.readValue(payload.getValue(), OrderUpdateMessage.class);
		assertThat(sent.qrToken()).isEqualTo(qrToken);
		assertThat(sent.order().tableLabel()).isEqualTo("A1");
	}
}
