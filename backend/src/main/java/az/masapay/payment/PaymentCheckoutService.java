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
import az.masapay.payment.payriff.PayriffException;
import az.masapay.payment.payriff.PayriffService;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.restaurant.RestaurantGate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(PaymentCheckoutService.class);

	/** Reason sent with the 502, so the phone can say Payriff is not answering. */
	public static final String PAYRIFF_UNAVAILABLE = "PAYRIFF_UNAVAILABLE";

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
		return checkoutFull(qrToken, null);
	}

	/** {@code tip} is on top of the bill (#181); null or zero for none. */
	public CheckoutResponse checkoutFull(UUID qrToken, BigDecimal tip) {
		gate.checkTakesPayments(qrToken);
		BigDecimal amount = settlementService.quoteFullPayment(qrToken).amount();
		if (amount.signum() <= 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has nothing left to pay");
		}
		return start(pending(qrToken, amount, tip, SplitType.FULL));
	}

	public CheckoutResponse checkoutItems(UUID qrToken, List<UUID> itemIds, Integer guests) {
		return checkoutItems(qrToken, itemIds, guests, null);
	}

	public CheckoutResponse checkoutItems(UUID qrToken, List<UUID> itemIds, Integer guests, BigDecimal tip) {
		gate.checkTakesPayments(qrToken);
		List<UUID> ids = itemIds.stream().distinct().toList();
		BigDecimal amount = settlementService.quoteItemPayment(qrToken, ids, guests).amount();
		Transaction transaction = pending(qrToken, amount, tip, SplitType.ITEM);
		transaction.setCheckoutItemIds(ids.stream().map(UUID::toString).collect(Collectors.joining(",")));
		transaction.setCheckoutGuests(guests);
		return start(transaction);
	}

	public CheckoutResponse checkoutEqual(UUID qrToken, int parts, int partsToPay) {
		return checkoutEqual(qrToken, parts, partsToPay, null);
	}

	public CheckoutResponse checkoutEqual(UUID qrToken, int parts, int partsToPay, BigDecimal tip) {
		gate.checkTakesPayments(qrToken);
		BigDecimal amount = settlementService.quoteEqualPayment(qrToken, parts, partsToPay).amount();
		Transaction transaction = pending(qrToken, amount, tip, SplitType.EQUAL);
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

	/**
	 * The tip a guest adds (#181): never negative, at most two decimals, and at most the bill part
	 * it comes with, which catches a slip of the finger (50.00 instead of 5.00).
	 */
	static BigDecimal checkedTip(BigDecimal tip, BigDecimal amount) {
		if (tip == null || tip.signum() == 0) {
			return BigDecimal.ZERO;
		}
		if (tip.signum() < 0 || tip.scale() > 2) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The tip must be a positive amount");
		}
		if (tip.compareTo(amount) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The tip is larger than the payment");
		}
		return tip.setScale(2);
	}

	private Transaction pending(UUID qrToken, BigDecimal amount, BigDecimal tip, SplitType splitType) {
		BigDecimal tipAmount = checkedTip(tip, amount);
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		Order order = orderRepository.findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), OrderStatus.OPEN)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open order for this table"));
		Transaction transaction = new Transaction();
		transaction.setOrder(order);
		transaction.setRestaurant(order.getRestaurant());
		// The bill part and the tip are kept apart: only the bill part pays the order off, and
		// Payriff charges both together.
		transaction.setAmount(amount);
		transaction.setTipAmount(tipAmount);
		transaction.setTotalAmount(amount.add(tipAmount));
		transaction.setCurrency(order.getCurrency());
		transaction.setSplitType(splitType);
		transaction.setStatus(TransactionStatus.PENDING);
		return transaction;
	}

	/**
	 * Saves the pending payment first (Payriff needs its id), then opens the Payriff page. If
	 * Payriff can not open it, the payment is marked FAILED, so no PENDING row is left behind to
	 * block the bill, and the guest gets 502 with {@value #PAYRIFF_UNAVAILABLE}.
	 */
	private CheckoutResponse start(Transaction transaction) {
		Transaction saved = transactionRepository.save(transaction);
		PayriffCheckout checkout;
		try {
			checkout = payriffService.startCheckout(saved);
		}
		catch (PayriffException e) {
			log.warn("Payriff could not open payment {}: {}", saved.getId(), e.getMessage());
			saved.setStatus(TransactionStatus.FAILED);
			transactionRepository.save(saved);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, PAYRIFF_UNAVAILABLE);
		}
		return new CheckoutResponse(saved.getId(), checkout.paymentUrl());
	}
}
