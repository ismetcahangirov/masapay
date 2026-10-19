package az.masapay.pos;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import az.masapay.payment.OrderPaidEvent;
import az.masapay.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PosOrderCloserTest {

	private final UUID orderId = UUID.randomUUID();
	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final PosAdapter iiko = mock(PosAdapter.class);
	private final Restaurant restaurant = new Restaurant();
	private final Order order = new Order();
	private PosOrderCloser closer;

	@BeforeEach
	void setUp() {
		when(iiko.type()).thenReturn(PosType.IIKO);
		closer = new PosOrderCloser(orderRepository, new PosAdapterRegistry(List.of(iiko)));
		restaurant.setPosType(PosType.IIKO);
		order.setRestaurant(restaurant);
		order.setTotalAmount(new BigDecimal("25.00"));
		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
	}

	@Test
	void closesTheBillInThePosWithTheBillTotal() {
		order.setPosOrderId("pos-1");

		closer.onOrderPaid(new OrderPaidEvent(orderId));

		verify(iiko).closeOrder(restaurant, "pos-1", new BigDecimal("25.00"));
	}

	@Test
	void skipsOrdersThatAreNotLinkedToAPos() {
		closer.onOrderPaid(new OrderPaidEvent(orderId));

		verify(iiko, never()).closeOrder(any(), any(), any());
	}

	@Test
	void aPosFailureDoesNotEscapeTheListener() {
		order.setPosOrderId("pos-1");
		doThrow(new IllegalStateException("iiko is down")).when(iiko).closeOrder(any(), any(), any());

		assertThatCode(() -> closer.onOrderPaid(new OrderPaidEvent(orderId))).doesNotThrowAnyException();
	}
}
