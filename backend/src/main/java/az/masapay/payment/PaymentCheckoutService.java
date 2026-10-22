package az.masapay.payment;

import az.masapay.domain.Order;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.dto.CheckoutResponse;
import az.masapay.payment.dto.PaymentStatusView;
import az.masapay.payment.payriff.PayriffCheckout;
import az.masapay.payment.payriff.PayriffService;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.restaurant.RestaurantGate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Starts a customer payment through Payriff (#37). The amount is quoted exactly as the direct
 * pay calls would charge it, saved as a PENDING transaction together with the chosen split, and
 * the guest is sent to Payriff's page. The split is applied only when Payriff confirms
 * ({@link PaymentSettlementService#settlePending}), so a cancelled payment changes nothing.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentCheckoutService {

	private final PaymentSettlementService settlementService;
	private final PayriffService payriffService;
	private final RestaurantTableRepository tableRepository;
	private final OrderRepository orderRepository;
	private final TransactionRepository transactionRepository;
	private final RestaurantGate gate;

	public PaymentCheckoutService(PaymentSettlementService settlementService, PayriffService payriffService,
			RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			TransactionRepository transactionRepository, RestaurantGate gate) {
		this.settlementService = settlementService;
		this.payriffService = payriffService;
		this.tableRepository = tableRepository;
		this.orderRepository = orderRepository;
		this.transactionRepository = transactionRepository;
		this.gate = gate;
	}

	public CheckoutResponse checkoutFull(UUID qrToken) {
		gate.checkTakesPayments(qrToken);
		BigDecimal amount = settlementService.quoteFullPayment(qrToken).amount();
		if (amount.signum() <= 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has nothing left to pay");
		}
		return start(pending(qrToken, amount, SplitType.FULL));
	}

	public CheckoutResponse checkoutItems(UUID qrToken, List<UUID> itemIds, Integer guests) {
		gate.checkTakesPayments(qrToken);
		List<UUID> ids = itemIds.stream().distinct().toList();
		BigDecimal amount = settlementService.quoteItemPayment(qrToken, ids, guests).amount();
		Transaction transaction = pending(qrToken, amount, SplitType.ITEM);
		transaction.setCheckoutItemIds(ids.stream().map(UUID::toString).collect(Collectors.joining(",")));
		transaction.setCheckoutGuests(guests);
		return start(transaction);
	}

	public CheckoutResponse checkoutEqual(UUID qrToken, int parts, int partsToPay) {
		gate.checkTakesPayments(qrToken);
		BigDecimal amount = settlementService.quoteEqualPayment(qrToken, parts, partsToPay).amount();
		Transaction transaction = pending(qrToken, amount, SplitType.EQUAL);
		transaction.setCheckoutParts(parts);
		transaction.setCheckoutPartsToPay(partsToPay);
		return start(transaction);
	}

	/** Status of a payment for the result page; the random transaction id is the access key. */
	@Transactional(readOnly = true)
	public PaymentStatusView status(UUID transactionId) {
		return transactionRepository.findById(transactionId)
			.map(PaymentStatusView::of)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
	}

	private Transaction pending(UUID qrToken, BigDecimal amount, SplitType splitType) {
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		Order order = orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), OrderStatus.OPEN)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open order for this table"));
		Transaction transaction = new Transaction();
		transaction.setOrder(order);
		transaction.setRestaurant(order.getRestaurant());
		transaction.setAmount(amount);
		transaction.setTotalAmount(amount);
		transaction.setCurrency(order.getCurrency());
		transaction.setSplitType(splitType);
		transaction.setStatus(TransactionStatus.PENDING);
		return transaction;
	}

	/** Saves the pending payment first (Payriff needs its id), then opens the Payriff page. */
	private CheckoutResponse start(Transaction transaction) {
		Transaction saved = transactionRepository.save(transaction);
		PayriffCheckout checkout = payriffService.startCheckout(saved);
		return new CheckoutResponse(saved.getId(), checkout.paymentUrl());
	}
}
