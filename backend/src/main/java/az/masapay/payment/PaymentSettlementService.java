package az.masapay.payment;

import az.masapay.domain.Order;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.dto.PaymentQuote;
import az.masapay.payment.dto.PaymentReceipt;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.split.SplitCalculationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Settles payments against a table's open order. #24 covers the FULL mode: pay the
 * whole outstanding amount and close the order.
 * <p>
 * NOTE: until Payriff lands (EPIC 7, #33-37) the confirm step marks the payment
 * SUCCESS directly. That epic will move the trigger to the Payriff webhook: create
 * a PENDING transaction, redirect to pay, then settle on confirmation.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentSettlementService {

	private final RestaurantTableRepository tableRepository;
	private final OrderRepository orderRepository;
	private final TransactionRepository transactionRepository;
	private final SplitCalculationService splitCalculationService;
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster;

	public PaymentSettlementService(RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			TransactionRepository transactionRepository, SplitCalculationService splitCalculationService,
			ObjectProvider<OrderRealtimeBroadcaster> broadcaster) {
		this.tableRepository = tableRepository;
		this.orderRepository = orderRepository;
		this.transactionRepository = transactionRepository;
		this.splitCalculationService = splitCalculationService;
		this.broadcaster = broadcaster;
	}

	/** The outstanding amount to settle the whole bill for this table's open order. */
	@Transactional(readOnly = true)
	public PaymentQuote quoteFullPayment(UUID qrToken) {
		Order order = openOrder(tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found")));
		return new PaymentQuote(outstanding(order), order.getCurrency());
	}

	/** Pays the full outstanding amount and marks the order PAID. */
	@Transactional
	public PaymentReceipt payFull(UUID qrToken) {
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		Order order = openOrder(table);

		BigDecimal amount = outstanding(order);
		if (amount.signum() <= 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has nothing left to pay");
		}

		Transaction transaction = new Transaction();
		transaction.setOrder(order);
		transaction.setRestaurant(order.getRestaurant());
		transaction.setAmount(amount);
		transaction.setTotalAmount(amount);
		transaction.setCurrency(order.getCurrency());
		transaction.setSplitType(SplitType.FULL);
		transaction.setStatus(TransactionStatus.SUCCESS);
		transaction.setPaidAt(Instant.now());
		transaction = transactionRepository.save(transaction);

		order.setStatus(OrderStatus.PAID);
		order.setClosedAt(Instant.now());
		orderRepository.save(order);

		// Best-effort real-time notification; absent when Redis is disabled.
		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(qrToken));

		return PaymentReceipt.of(transaction, order);
	}

	private Order openOrder(RestaurantTable table) {
		return orderRepository
			.findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), OrderStatus.OPEN)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open order for this table"));
	}

	private BigDecimal outstanding(Order order) {
		BigDecimal paid = transactionRepository
			.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS).stream()
			.map(Transaction::getTotalAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return splitCalculationService.calculateFullPayment(order.getTotalAmount(), paid);
	}
}
