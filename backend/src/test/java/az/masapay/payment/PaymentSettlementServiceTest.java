package az.masapay.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.dto.PaymentReceipt;
import az.masapay.realtime.OrderRealtimeBroadcaster;
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
		service = new PaymentSettlementService(
			tableRepository, orderRepository, transactionRepository, new SplitCalculationService(), broadcaster);

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
}
