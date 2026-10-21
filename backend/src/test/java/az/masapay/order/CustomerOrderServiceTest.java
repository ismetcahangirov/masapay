package az.masapay.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

	@Mock
	private RestaurantTableRepository tableRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderItemRepository orderItemRepository;
	@Mock
	private TransactionRepository transactionRepository;

	private CustomerOrderService service;

	private final UUID qrToken = UUID.randomUUID();
	private final UUID tableId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new CustomerOrderService(tableRepository, orderRepository, orderItemRepository,
			transactionRepository);
	}

	private RestaurantTable table() {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		restaurant.setName("Cafe");
		restaurant.setCurrency("AZN");
		RestaurantTable table = new RestaurantTable();
		table.setId(tableId);
		table.setLabel("A1");
		table.setRestaurant(restaurant);
		return table;
	}

	@Test
	void returnsOrderWithItemsWhenOpenOrderExists() {
		RestaurantTable table = table();
		Order order = new Order();
		order.setId(UUID.randomUUID());
		order.setStatus(OrderStatus.OPEN);
		order.setTotalAmount(new BigDecimal("7.00"));
		OrderItem item = new OrderItem();
		item.setId(UUID.randomUUID());
		item.setName("Espresso");
		item.setQuantity(new BigDecimal("2.000"));
		item.setUnitPrice(new BigDecimal("3.50"));
		item.setTotalPrice(new BigDecimal("7.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(item));

		TableOrderResponse response = service.getCurrentOrder(qrToken);

		assertThat(response.tableLabel()).isEqualTo("A1");
		assertThat(response.restaurantName()).isEqualTo("Cafe");
		assertThat(response.currency()).isEqualTo("AZN");
		assertThat(response.order()).isNotNull();
		assertThat(response.order().totalAmount()).isEqualByComparingTo("7.00");
		assertThat(response.order().items()).singleElement()
			.satisfies(i -> assertThat(i.name()).isEqualTo("Espresso"));
	}

	@Test
	void returnsNullOrderWhenNoOpenOrder() {
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table()));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.empty());

		TableOrderResponse response = service.getCurrentOrder(qrToken);

		assertThat(response.tableLabel()).isEqualTo("A1");
		assertThat(response.order()).isNull();
	}

	@Test
	void showsARecentlyPaidBillAsAReceiptWhenNoBillIsOpen() {
		Order paid = new Order();
		paid.setId(UUID.randomUUID());
		paid.setStatus(OrderStatus.PAID);
		paid.setTotalAmount(new BigDecimal("7.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table()));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.empty());
		when(orderRepository.findFirstByTableIdAndStatusAndClosedAtAfterOrderByClosedAtDesc(
				eq(tableId), eq(OrderStatus.PAID), any(Instant.class)))
			.thenReturn(Optional.of(paid));

		TableOrderResponse response = service.getCurrentOrder(qrToken);

		assertThat(response.order()).isNotNull();
		assertThat(response.order().status()).isEqualTo("PAID");
	}

	@Test
	void reportsPaidAndOutstandingFromSuccessfulPaymentsAcrossSplitModes() {
		Order order = new Order();
		order.setId(UUID.randomUUID());
		order.setStatus(OrderStatus.OPEN);
		order.setTotalAmount(new BigDecimal("27.00"));
		Transaction itemPayment = new Transaction();
		itemPayment.setAmount(new BigDecimal("9.00"));
		Transaction equalShare = new Transaction();
		equalShare.setAmount(new BigDecimal("9.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table()));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of(itemPayment, equalShare));

		TableOrderResponse response = service.getCurrentOrder(qrToken);

		assertThat(response.order().paidAmount()).isEqualByComparingTo("18.00");
		assertThat(response.order().outstanding()).isEqualByComparingTo("9.00");
	}

	@Test
	void unknownTokenIsNotFound() {
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getCurrentOrder(qrToken)).isInstanceOf(ResponseStatusException.class);
	}
}
