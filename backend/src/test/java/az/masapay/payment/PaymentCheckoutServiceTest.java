package az.masapay.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.dto.CheckoutResponse;
import az.masapay.payment.dto.EqualPaymentQuote;
import az.masapay.payment.dto.PaymentQuote;
import az.masapay.payment.dto.PaymentStatusView;
import az.masapay.payment.payriff.PayriffCheckout;
import az.masapay.payment.payriff.PayriffService;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.restaurant.RestaurantGate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PaymentCheckoutServiceTest {

	private final PaymentSettlementService settlementService = mock(PaymentSettlementService.class);
	private final PayriffService payriffService = mock(PayriffService.class);
	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final PaymentCheckoutService service = new PaymentCheckoutService(settlementService, payriffService,
			tableRepository, orderRepository, transactionRepository, new RestaurantGate(tableRepository));

	private final UUID qrToken = UUID.randomUUID();
	private final RestaurantTable table = new RestaurantTable();
	private final Order order = new Order();

	@BeforeEach
	void setUp() {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		when(tableRepository.findRestaurantByQrToken(qrToken)).thenReturn(Optional.of(restaurant));
		order.setId(UUID.randomUUID());
		order.setRestaurant(restaurant);
		order.setTable(table);
		order.setCurrency("AZN");
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
			Transaction transaction = inv.getArgument(0);
			transaction.setId(UUID.randomUUID());
			return transaction;
		});
		when(payriffService.startCheckout(any())).thenReturn(new PayriffCheckout("ord-1", "https://pay/ord-1"));
	}

	private Transaction saved() {
		ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void aFullCheckoutSavesAPendingPaymentAndReturnsThePayriffPage() {
		when(settlementService.quoteFullPayment(qrToken)).thenReturn(new PaymentQuote(new BigDecimal("25.00"), "AZN"));

		CheckoutResponse response = service.checkoutFull(qrToken);

		Transaction pending = saved();
		assertThat(response.paymentUrl()).isEqualTo("https://pay/ord-1");
		assertThat(response.transactionId()).isEqualTo(pending.getId());
		assertThat(pending.getStatus()).isEqualTo(TransactionStatus.PENDING);
		assertThat(pending.getSplitType()).isEqualTo(SplitType.FULL);
		assertThat(pending.getAmount()).isEqualByComparingTo("25.00");
	}

	@Test
	void anItemCheckoutRemembersTheItemsAndGuests() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		when(settlementService.quoteItemPayment(qrToken, List.of(a, b), 3))
			.thenReturn(new PaymentQuote(new BigDecimal("11.50"), "AZN"));

		service.checkoutItems(qrToken, List.of(a, b, a), 3);

		Transaction pending = saved();
		assertThat(pending.getCheckoutItemIds()).isEqualTo(a + "," + b);
		assertThat(pending.getCheckoutGuests()).isEqualTo(3);
	}

	@Test
	void anEqualCheckoutRemembersTheParts() {
		when(settlementService.quoteEqualPayment(qrToken, 3, 2))
			.thenReturn(new EqualPaymentQuote(new BigDecimal("16.67"), "AZN", 3, 0));

		service.checkoutEqual(qrToken, 3, 2);

		Transaction pending = saved();
		assertThat(pending.getCheckoutParts()).isEqualTo(3);
		assertThat(pending.getCheckoutPartsToPay()).isEqualTo(2);
	}

	@Test
	void nothingToPayIsAConflict() {
		when(settlementService.quoteFullPayment(qrToken)).thenReturn(new PaymentQuote(BigDecimal.ZERO, "AZN"));

		assertThatThrownBy(() -> service.checkoutFull(qrToken))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void theStatusShowsThePaymentAndItsTable() {
		Transaction transaction = new Transaction();
		transaction.setId(UUID.randomUUID());
		transaction.setOrder(order);
		transaction.setRestaurant(order.getRestaurant());
		transaction.setTotalAmount(new BigDecimal("25.00"));
		transaction.setStatus(TransactionStatus.SUCCESS);
		when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

		PaymentStatusView status = service.status(transaction.getId());

		assertThat(status.status()).isEqualTo("SUCCESS");
		assertThat(status.qrToken()).isEqualTo(table.getQrToken());
	}
}
