package az.masapay.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.dto.PaymentReceipt;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.split.SplitCalculationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementServiceTest {

	@Mock
	private RestaurantTableRepository tableRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderItemRepository orderItemRepository;
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private ObjectProvider<OrderRealtimeBroadcaster> broadcaster;

	private PaymentSettlementService service;

	private final UUID qrToken = UUID.randomUUID();
	private final UUID tableId = UUID.randomUUID();
	private RestaurantTable table;
	private Order order;

	@BeforeEach
	void setUp() {
		service = new PaymentSettlementService(tableRepository, orderRepository, orderItemRepository,
			transactionRepository, new SplitCalculationService(), broadcaster);

		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		restaurant.setCurrency("AZN");
		table = new RestaurantTable();
		table.setId(tableId);
		table.setRestaurant(restaurant);
		order = new Order();
		order.setId(UUID.randomUUID());
		order.setRestaurant(restaurant);
		order.setStatus(OrderStatus.OPEN);
		order.setTotalAmount(new BigDecimal("25.00"));
		order.setCurrency("AZN");
	}

	@Test
	void payFullCreatesSuccessTransactionAndMarksOrderPaid() {
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of());
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		PaymentReceipt receipt = service.payFull(qrToken);

		ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(txCaptor.capture());
		Transaction saved = txCaptor.getValue();
		assertThat(saved.getTotalAmount()).isEqualByComparingTo("25.00");
		assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		assertThat(saved.getSplitType()).isEqualTo(SplitType.FULL);
		assertThat(saved.getPaidAt()).isNotNull();

		verify(orderRepository).save(order);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getClosedAt()).isNotNull();

		assertThat(receipt.orderStatus()).isEqualTo("PAID");
		assertThat(receipt.amount()).isEqualByComparingTo("25.00");
	}

	@Test
	void payFullOnlyChargesTheOutstandingRemainder() {
		Transaction prior = new Transaction();
		prior.setTotalAmount(new BigDecimal("10.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of(prior));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		PaymentReceipt receipt = service.payFull(qrToken);

		assertThat(receipt.amount()).isEqualByComparingTo("15.00");
	}

	@Test
	void payFullWithoutOpenOrderIsConflict() {
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.payFull(qrToken)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void payFullWhenAlreadySettledIsConflict() {
		Transaction prior = new Transaction();
		prior.setTotalAmount(new BigDecimal("25.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of(prior));

		assertThatThrownBy(() -> service.payFull(qrToken)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void unknownTokenIsNotFound() {
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.payFull(qrToken)).isInstanceOf(ResponseStatusException.class);
	}

	private OrderItem orderItem(UUID id, String price, boolean paid) {
		OrderItem item = new OrderItem();
		item.setId(id);
		item.setOrder(order);
		item.setTotalPrice(new BigDecimal(price));
		item.setPaid(paid);
		return item;
	}

	@Test
	void payItemsChargesSelectedSumAndKeepsOrderOpenWhenItemsRemain() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		List<UUID> ids = List.of(id1, id2);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids))
			.thenReturn(List.of(orderItem(id1, "7.00", false), orderItem(id2, "4.50", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(org.mockito.ArgumentMatchers.eq(ids), org.mockito.ArgumentMatchers.eq(order.getId()), org.mockito.ArgumentMatchers.any())).thenReturn(2);
		when(orderItemRepository.existsByOrderIdAndPaidFalse(order.getId())).thenReturn(true);

		PaymentReceipt receipt = service.payItems(qrToken, ids);

		assertThat(receipt.amount()).isEqualByComparingTo("11.50");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
	}

	@Test
	void payItemsMarksOrderPaidWhenNoUnpaidItemsRemain() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "25.00", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(org.mockito.ArgumentMatchers.eq(ids), org.mockito.ArgumentMatchers.eq(order.getId()), org.mockito.ArgumentMatchers.any())).thenReturn(1);
		when(orderItemRepository.existsByOrderIdAndPaidFalse(order.getId())).thenReturn(false);

		service.payItems(qrToken, ids);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getClosedAt()).isNotNull();
	}

	@Test
	void payItemsRejectsConcurrentClaimWhenFewerRowsUpdated() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		List<UUID> ids = List.of(id1, id2);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids))
			.thenReturn(List.of(orderItem(id1, "7.00", false), orderItem(id2, "4.50", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		// Only one row claimed: the other item was settled concurrently.
		when(orderItemRepository.claimItems(org.mockito.ArgumentMatchers.eq(ids), org.mockito.ArgumentMatchers.eq(order.getId()), org.mockito.ArgumentMatchers.any())).thenReturn(1);

		assertThatThrownBy(() -> service.payItems(qrToken, ids)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void payItemsRejectsAlreadyPaidItemUpFront() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "7.00", true)));

		assertThatThrownBy(() -> service.payItems(qrToken, ids)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void payItemsRejectsUnknownItem() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of());

		assertThatThrownBy(() -> service.payItems(qrToken, ids)).isInstanceOf(ResponseStatusException.class);
	}
}
