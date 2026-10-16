package az.masapay.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import az.masapay.payment.dto.EqualPaymentQuote;
import az.masapay.payment.dto.PaymentQuote;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
		when(orderRepository.findOpenOrderForUpdate(tableId))
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
		prior.setAmount(new BigDecimal("10.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.of(order));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of(prior));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		PaymentReceipt receipt = service.payFull(qrToken);

		assertThat(receipt.amount()).isEqualByComparingTo("15.00");
	}

	@Test
	void earlierTipsDoNotPayTheBillDown() {
		Transaction prior = new Transaction();
		prior.setAmount(new BigDecimal("10.00"));
		prior.setTipAmount(new BigDecimal("2.00"));
		prior.setTotalAmount(new BigDecimal("12.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of(prior));

		PaymentQuote quote = service.quoteFullPayment(qrToken);

		assertThat(quote.amount()).isEqualByComparingTo("15.00");
	}

	@Test
	void payFullWithoutOpenOrderIsConflict() {
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.payFull(qrToken)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void payFullWhenAlreadySettledIsConflict() {
		Transaction prior = new Transaction();
		prior.setAmount(new BigDecimal("25.00"));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
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
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids))
			.thenReturn(List.of(orderItem(id1, "7.00", false), orderItem(id2, "4.50", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(org.mockito.ArgumentMatchers.eq(ids), org.mockito.ArgumentMatchers.eq(order.getId()), org.mockito.ArgumentMatchers.any())).thenReturn(2);
		// The 25.00 bill is exactly its items: no service charge.
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(id1, "7.00", false), orderItem(id2, "4.50", false), orderItem(UUID.randomUUID(), "13.50", false)));

		PaymentReceipt receipt = service.payItems(qrToken, ids, null);

		assertThat(receipt.amount()).isEqualByComparingTo("11.50");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
	}

	@Test
	void payItemsMarksOrderPaidWhenNoUnpaidItemsRemain() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "25.00", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(org.mockito.ArgumentMatchers.eq(ids), org.mockito.ArgumentMatchers.eq(order.getId()), org.mockito.ArgumentMatchers.any())).thenReturn(1);
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(orderItem(id1, "25.00", false)));

		service.payItems(qrToken, ids, null);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getClosedAt()).isNotNull();
	}

	@Test
	void payItemsRejectsConcurrentClaimWhenFewerRowsUpdated() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		List<UUID> ids = List.of(id1, id2);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids))
			.thenReturn(List.of(orderItem(id1, "7.00", false), orderItem(id2, "4.50", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(id1, "7.00", false), orderItem(id2, "4.50", false), orderItem(UUID.randomUUID(), "13.50", false)));
		// Only one row claimed: the other item was settled concurrently.
		when(orderItemRepository.claimItems(org.mockito.ArgumentMatchers.eq(ids), org.mockito.ArgumentMatchers.eq(order.getId()), org.mockito.ArgumentMatchers.any())).thenReturn(1);

		assertThatThrownBy(() -> service.payItems(qrToken, ids, null)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void payItemsRejectsAlreadyPaidItemUpFront() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "7.00", true)));

		assertThatThrownBy(() -> service.payItems(qrToken, ids, null)).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void payItemsRejectsUnknownItem() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId))
			.thenReturn(Optional.of(order));
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of());

		assertThatThrownBy(() -> service.payItems(qrToken, ids, null)).isInstanceOf(ResponseStatusException.class);
	}
	private void openOrderWithTotal(String total, String alreadyPaid) {
		orderWithTotal(total, alreadyPaid);
		when(orderRepository.findOpenOrderForUpdate(tableId)).thenReturn(Optional.of(order));
	}

	/** Same as {@link #openOrderWithTotal} for read-only quotes, which do not lock the order. */
	private void quotedOrderWithTotal(String total, String alreadyPaid) {
		orderWithTotal(total, alreadyPaid);
		when(orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(tableId, OrderStatus.OPEN))
			.thenReturn(Optional.of(order));
	}

	private void orderWithTotal(String total, String alreadyPaid) {
		order.setTotalAmount(new BigDecimal(total));
		Transaction prior = new Transaction();
		prior.setAmount(new BigDecimal(alreadyPaid));
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS))
			.thenReturn(List.of(prior));
	}

	private void equalSplitInProgress(int parts, String base, int paidParts) {
		order.setEqualSplitParts(parts);
		order.setEqualSplitBase(new BigDecimal(base));
		order.setEqualSplitPaidParts(paidParts);
	}

	private static HttpStatusCode statusOf(Throwable e) {
		return ((ResponseStatusException) e).getStatusCode();
	}

	@Test
	void payEqualFirstShareFixesTheSplitAndKeepsOrderOpen() {
		openOrderWithTotal("100.00", "0.00");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(3), argThat(b -> b.compareTo(new BigDecimal("100.00")) == 0),
			eq(0), eq(1))).thenReturn(1);

		PaymentReceipt receipt = service.payEqual(qrToken, 3, 1);

		ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(txCaptor.capture());
		assertThat(txCaptor.getValue().getSplitType()).isEqualTo(SplitType.EQUAL);
		// 100.00 / 3: the leftover cent goes to the first share (33.34 + 33.33 + 33.33).
		assertThat(receipt.amount()).isEqualByComparingTo("33.34");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
		assertThat(order.getEqualSplitParts()).isEqualTo(3);
		assertThat(order.getEqualSplitBase()).isEqualByComparingTo("100.00");
		assertThat(order.getEqualSplitPaidParts()).isEqualTo(1);
	}

	@Test
	void payEqualLaterShareUsesTheLockedSplitAmount() {
		// Splitting the 66.66 still outstanding by 3 again would give 22.22; the locked split keeps it fair.
		openOrderWithTotal("100.00", "33.34");
		equalSplitInProgress(3, "100.00", 1);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(3), any(), eq(1), eq(2))).thenReturn(1);

		PaymentReceipt receipt = service.payEqual(qrToken, 3, 1);

		assertThat(receipt.amount()).isEqualByComparingTo("33.33");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
	}

	@Test
	void payEqualMultiplePartsChargesTheirSum() {
		openOrderWithTotal("100.00", "0.00");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(3), any(), eq(0), eq(2))).thenReturn(1);

		PaymentReceipt receipt = service.payEqual(qrToken, 3, 2);

		assertThat(receipt.amount()).isEqualByComparingTo("66.67");
	}

	@Test
	void payEqualFinalShareSettlesExactRemainderAndClosesOrder() {
		openOrderWithTotal("100.00", "66.67");
		equalSplitInProgress(3, "100.00", 2);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(3), any(), eq(2), eq(3))).thenReturn(1);

		PaymentReceipt receipt = service.payEqual(qrToken, 3, 1);

		assertThat(receipt.amount()).isEqualByComparingTo("33.33");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getClosedAt()).isNotNull();
		verify(orderRepository).save(order);
	}

	@Test
	void payEqualRejectsDifferentPartCountOnceSplitIsLocked() {
		openOrderWithTotal("100.00", "33.34");
		equalSplitInProgress(3, "100.00", 1);

		assertThatThrownBy(() -> service.payEqual(qrToken, 4, 1))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void payEqualAllowsRestartingTheSplitBeforeAnyPartIsPaid() {
		openOrderWithTotal("90.00", "0.00");
		equalSplitInProgress(3, "90.00", 0);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(2), any(), eq(0), eq(1))).thenReturn(1);

		PaymentReceipt receipt = service.payEqual(qrToken, 2, 1);

		assertThat(receipt.amount()).isEqualByComparingTo("45.00");
	}

	@Test
	void payEqualRejectsMorePartsThanRemain() {
		openOrderWithTotal("100.00", "66.67");
		equalSplitInProgress(3, "100.00", 2);

		assertThatThrownBy(() -> service.payEqual(qrToken, 3, 2))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void payEqualRejectsConcurrentClaim() {
		openOrderWithTotal("100.00", "0.00");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		// Another diner claimed a part between our read and our update.
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(3), any(), eq(0), eq(1))).thenReturn(0);

		assertThatThrownBy(() -> service.payEqual(qrToken, 3, 1))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
		assertThat(order.getEqualSplitParts()).isNull();
	}

	@Test
	void payEqualWhenNothingLeftIsConflict() {
		openOrderWithTotal("100.00", "100.00");

		assertThatThrownBy(() -> service.payEqual(qrToken, 3, 1))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void quoteEqualReportsShareAndProgressWithoutPaying() {
		quotedOrderWithTotal("100.00", "33.34");
		equalSplitInProgress(3, "100.00", 1);

		EqualPaymentQuote quote = service.quoteEqualPayment(qrToken, 3, 2);

		assertThat(quote.amount()).isEqualByComparingTo("66.66");
		assertThat(quote.totalParts()).isEqualTo(3);
		assertThat(quote.paidParts()).isEqualTo(1);
		verify(transactionRepository, never()).save(any());
		verify(orderRepository, never()).claimEqualParts(any(), anyInt(), any(), anyInt(), anyInt());
	}

	@Test
	void payFullLoadsTheOrderWithARowLock() {
		openOrderWithTotal("25.00", "0.00");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		service.payFull(qrToken);

		// The locked read serializes concurrent payers; the unlocked one would let both charge the bill.
		verify(orderRepository).findOpenOrderForUpdate(tableId);
		verify(orderRepository, never()).findFirstByTableIdAndStatusOrderByOpenedAtDesc(any(), any());
	}

	@Test
	void payItemsIsRejectedOnceEqualSharesArePaid() {
		UUID id1 = UUID.randomUUID();
		when(tableRepository.findByQrToken(qrToken)).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(tableId)).thenReturn(Optional.of(order));
		equalSplitInProgress(2, "25.00", 1);

		assertThatThrownBy(() -> service.payItems(qrToken, List.of(id1), null))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void payItemsNeverChargesMoreThanIsOutstandingAndThenClosesTheOrder() {
		// 20.00 of the 25.00 bill is already paid; the selected item costs 7.00.
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("25.00", "20.00");
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "7.00", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(eq(ids), eq(order.getId()), any())).thenReturn(1);
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(id1, "7.00", false), orderItem(UUID.randomUUID(), "5.00", false), orderItem(UUID.randomUUID(), "13.00", true)));

		PaymentReceipt receipt = service.payItems(qrToken, ids, null);

		assertThat(receipt.amount()).isEqualByComparingTo("5.00");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	void payItemsWhenNothingLeftIsConflict() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("25.00", "25.00");
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "7.00", false)));

		assertThatThrownBy(() -> service.payItems(qrToken, ids, null))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
		verify(transactionRepository, never()).save(any());
	}
	@Test
	void payItemsAddsOneGuestsEqualShareOfTheServiceCharge() {
		// 50.00 bill = 42.00 of items + 8.00 service charge, shared by 4 guests: 2.00 each.
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("50.00", "0.00");
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "12.00", false)));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(id1, "12.00", false), orderItem(UUID.randomUUID(), "30.00", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(eq(ids), eq(order.getId()), any())).thenReturn(1);

		PaymentReceipt receipt = service.payItems(qrToken, ids, 4);

		assertThat(receipt.amount()).isEqualByComparingTo("14.00");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
		assertThat(order.getServiceChargeGuests()).isEqualTo(4);
		assertThat(order.getServiceChargePaidShares()).isEqualTo(1);
	}

	@Test
	void payItemsUsesTheGuestCountFixedByTheFirstPayer() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("50.00", "14.00");
		order.setServiceChargeGuests(4);
		order.setServiceChargePaidShares(1);
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "10.00", false)));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(UUID.randomUUID(), "12.00", true), orderItem(id1, "10.00", false),
			orderItem(UUID.randomUUID(), "20.00", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(eq(ids), eq(order.getId()), any())).thenReturn(1);

		PaymentReceipt receipt = service.payItems(qrToken, ids, null);

		assertThat(receipt.amount()).isEqualByComparingTo("12.00");
		assertThat(order.getServiceChargePaidShares()).isEqualTo(2);
	}

	@Test
	void payItemsLastItemsSettleWhateverServiceChargeIsLeft() {
		// Only 2 of the 4 guests paid a share; the last payer covers the remaining 4.00 too.
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("50.00", "26.00");
		order.setServiceChargeGuests(4);
		order.setServiceChargePaidShares(2);
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "20.00", false)));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(UUID.randomUUID(), "12.00", true), orderItem(UUID.randomUUID(), "10.00", true),
			orderItem(id1, "20.00", false)));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderItemRepository.claimItems(eq(ids), eq(order.getId()), any())).thenReturn(1);

		PaymentReceipt receipt = service.payItems(qrToken, ids, null);

		assertThat(receipt.amount()).isEqualByComparingTo("24.00");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getServiceChargePaidShares()).isEqualTo(4);
	}

	@Test
	void payItemsRequiresAGuestCountWhenTheBillHasAServiceCharge() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("50.00", "0.00");
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "12.00", false)));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(id1, "12.00", false), orderItem(UUID.randomUUID(), "30.00", false)));

		assertThatThrownBy(() -> service.payItems(qrToken, ids, null))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void payItemsRejectsADifferentGuestCountOnceFixed() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		openOrderWithTotal("50.00", "14.00");
		order.setServiceChargeGuests(4);
		order.setServiceChargePaidShares(1);
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "10.00", false)));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(UUID.randomUUID(), "12.00", true), orderItem(id1, "10.00", false),
			orderItem(UUID.randomUUID(), "20.00", false)));

		assertThatThrownBy(() -> service.payItems(qrToken, ids, 3))
			.satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void quoteItemsIncludesTheServiceChargeShareWithoutPaying() {
		UUID id1 = UUID.randomUUID();
		List<UUID> ids = List.of(id1);
		quotedOrderWithTotal("50.00", "0.00");
		when(orderItemRepository.findAllById(ids)).thenReturn(List.of(orderItem(id1, "12.00", false)));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(
			orderItem(id1, "12.00", false), orderItem(UUID.randomUUID(), "30.00", false)));

		PaymentQuote quote = service.quoteItemPayment(qrToken, ids, 4);

		assertThat(quote.amount()).isEqualByComparingTo("14.00");
		assertThat(order.getServiceChargeGuests()).isNull();
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void payEqualSplitsTheWholeBillIncludingServiceChargeByHeadcount() {
		// The 50.00 bill includes a service charge on top of the items; 4 diners each pay 50.00 / 4.
		openOrderWithTotal("50.00", "0.00");
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
		when(orderRepository.claimEqualParts(eq(order.getId()), eq(4), argThat(b -> b.compareTo(new BigDecimal("50.00")) == 0),
			eq(0), eq(1))).thenReturn(1);

		PaymentReceipt receipt = service.payEqual(qrToken, 4, 1);

		assertThat(receipt.amount()).isEqualByComparingTo("12.50");
	}
}
