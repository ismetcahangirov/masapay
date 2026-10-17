package az.masapay.payment;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Settles payments against a table's open order in the three split modes: FULL
 * (#24) pays the whole outstanding amount, ITEM (#25) pays selected line items and
 * EQUAL (#26) pays equal shares of the bill.
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
	private final OrderItemRepository orderItemRepository;
	private final TransactionRepository transactionRepository;
	private final SplitCalculationService splitCalculationService;
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster;

	public PaymentSettlementService(RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			OrderItemRepository orderItemRepository, TransactionRepository transactionRepository,
			SplitCalculationService splitCalculationService, ObjectProvider<OrderRealtimeBroadcaster> broadcaster) {
		this.tableRepository = tableRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
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
		Order order = openOrderForUpdate(table);

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

	/**
	 * ITEM ("Öz Yediyini Ödə"): pays for the selected line items. The items are
	 * claimed with a single conditional update, so if another payer settled one of
	 * them first this call is rejected with 409 and rolled back (race protection).
	 * See {@link #planItemPayment} for how the amount is worked out.
	 */
	@Transactional
	public PaymentReceipt payItems(UUID qrToken, List<UUID> itemIds, Integer guests) {
		List<UUID> ids = itemIds.stream().distinct().toList();
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		Order order = openOrderForUpdate(table);
		ItemPaymentPlan plan = planItemPayment(order, ids, guests);

		Transaction transaction = new Transaction();
		transaction.setOrder(order);
		transaction.setRestaurant(order.getRestaurant());
		transaction.setAmount(plan.amount());
		transaction.setTotalAmount(plan.amount());
		transaction.setCurrency(order.getCurrency());
		transaction.setSplitType(SplitType.ITEM);
		transaction.setStatus(TransactionStatus.SUCCESS);
		transaction.setPaidAt(Instant.now());
		transaction = transactionRepository.save(transaction);

		int claimed = orderItemRepository.claimItems(ids, order.getId(), transaction);
		if (claimed != ids.size()) {
			// A concurrent payer settled one of these items first; abort and roll back.
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Some items were just paid by someone else");
		}

		order.setServiceChargeGuests(plan.serviceChargeGuests());
		order.setServiceChargePaidShares(plan.serviceChargePaidShares());
		if (plan.settlesOrder()) {
			order.setStatus(OrderStatus.PAID);
			order.setClosedAt(Instant.now());
		}
		orderRepository.save(order);

		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(qrToken));
		return PaymentReceipt.of(transaction, order);
	}

	/** The amount for the selected line items, without paying (it may include the bill's extra charges). */
	@Transactional(readOnly = true)
	public PaymentQuote quoteItemPayment(UUID qrToken, List<UUID> itemIds, Integer guests) {
		Order order = openOrder(tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found")));
		ItemPaymentPlan plan = planItemPayment(order, itemIds.stream().distinct().toList(), guests);
		return new PaymentQuote(plan.amount(), order.getCurrency());
	}

	/**
	 * Validates the selected items and works out what they cost: the sum of their
	 * prices plus, when the bill has a service charge (its total is more than its
	 * items), one guest's equal share of that charge. The first item payer fixes
	 * how many guests share it; shares come from
	 * {@link SplitCalculationService#splitEqually}, so they sum exactly to the
	 * charge. The payer whose selection covers every item still unpaid settles the
	 * whole outstanding balance, so the order never closes with money owed (e.g.
	 * when fewer guests paid than the count fixed). No payment is ever more than is
	 * still owed. Item payment is refused once equal shares have been paid.
	 */
	private ItemPaymentPlan planItemPayment(Order order, List<UUID> ids, Integer guests) {
		if (order.getEqualSplitPaidParts() > 0) {
			// Equal shares are not tied to line items, so paying items now would charge them a second time.
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				"The bill is being split equally; pay an equal share instead");
		}

		List<OrderItem> items = orderItemRepository.findAllById(ids);
		if (items.size() != ids.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some items do not exist");
		}
		for (OrderItem item : items) {
			if (!item.getOrder().getId().equals(order.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this order");
			}
			if (item.isPaid()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Some items are already paid");
			}
		}

		BigDecimal outstanding = outstanding(order);
		if (outstanding.signum() <= 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has nothing left to pay");
		}

		List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
		BigDecimal serviceCharge = splitCalculationService.calculateServiceCharge(
			order.getTotalAmount(), orderItems.stream().map(OrderItem::getTotalPrice).collect(Collectors.toList()));
		Integer chargeGuests = order.getServiceChargeGuests();
		int paidShares = order.getServiceChargePaidShares();
		BigDecimal share = BigDecimal.ZERO;
		if (serviceCharge.signum() > 0) {
			chargeGuests = resolveServiceChargeGuests(order, guests);
			if (paidShares < chargeGuests) {
				share = splitCalculationService.splitEqually(serviceCharge, chargeGuests).get(paidShares);
				paidShares++;
			}
		}

		long unpaidItems = orderItems.stream().filter(i -> !i.isPaid()).count();
		boolean lastItems = ids.size() == unpaidItems;
		BigDecimal amount = lastItems
			? outstanding
			// Never charge more than is still owed on the bill.
			: splitCalculationService.calculateItemPayment(
				items.stream().map(OrderItem::getTotalPrice).collect(Collectors.toList())).add(share).min(outstanding);
		if (lastItems && serviceCharge.signum() > 0) {
			// The last payer covers any shares left unpaid.
			paidShares = chargeGuests;
		}
		return new ItemPaymentPlan(amount, lastItems || amount.compareTo(outstanding) == 0, chargeGuests, paidShares);
	}

	/**
	 * The guest count the service charge is shared by: the one already fixed for
	 * this order, or the payer's count if they are the first. A different count
	 * once it is fixed is rejected so every guest pays the same share.
	 */
	private int resolveServiceChargeGuests(Order order, Integer guests) {
		Integer fixed = order.getServiceChargeGuests();
		if (fixed != null) {
			if (guests != null && !guests.equals(fixed)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The service charge is already shared by " + fixed + " guests");
			}
			return fixed;
		}
		if (guests == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"guests is required: the bill has a service charge to share");
		}
		return guests;
	}

	private record ItemPaymentPlan(
			BigDecimal amount, boolean settlesOrder, Integer serviceChargeGuests, int serviceChargePaidShares) {
	}

	/** The amount for {@code partsToPay} of {@code parts} equal shares, without paying. */
	@Transactional(readOnly = true)
	public EqualPaymentQuote quoteEqualPayment(UUID qrToken, int parts, int partsToPay) {
		Order order = openOrder(tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found")));
		EqualSharePlan plan = planEqualShares(order, parts, partsToPay, outstanding(order));
		return new EqualPaymentQuote(plan.amount(), order.getCurrency(), parts, plan.paidBefore());
	}

	/**
	 * EQUAL ("Bərabər Böl"): pays {@code partsToPay} of {@code parts} equal shares.
	 * The first payment fixes the part count and the amount being split, so every
	 * later payer gets a share of that same split. Shares come from
	 * {@link SplitCalculationService#splitEqually}, so they sum exactly to the split
	 * amount; the final share settles the exact outstanding balance, absorbing any
	 * rounding difference. Paid parts are claimed with a compare-and-set, so a
	 * concurrent payer is rejected with 409 and rolled back.
	 */
	@Transactional
	public PaymentReceipt payEqual(UUID qrToken, int parts, int partsToPay) {
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		Order order = openOrderForUpdate(table);
		EqualSharePlan plan = planEqualShares(order, parts, partsToPay, outstanding(order));

		Transaction transaction = new Transaction();
		transaction.setOrder(order);
		transaction.setRestaurant(order.getRestaurant());
		transaction.setAmount(plan.amount());
		transaction.setTotalAmount(plan.amount());
		transaction.setCurrency(order.getCurrency());
		transaction.setSplitType(SplitType.EQUAL);
		transaction.setStatus(TransactionStatus.SUCCESS);
		transaction.setPaidAt(Instant.now());
		transaction = transactionRepository.save(transaction);

		int claimed = orderRepository.claimEqualParts(
			order.getId(), parts, plan.base(), plan.paidBefore(), plan.paidAfter());
		if (claimed != 1) {
			// Another payer claimed a part (or changed the split) first; abort and roll back.
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The bill split was just updated by someone else");
		}
		// Keep the managed entity in step with the conditional update before it is flushed.
		order.setEqualSplitParts(parts);
		order.setEqualSplitBase(plan.base());
		order.setEqualSplitPaidParts(plan.paidAfter());

		if (plan.settlesOrder()) {
			order.setStatus(OrderStatus.PAID);
			order.setClosedAt(Instant.now());
		}
		orderRepository.save(order);

		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(qrToken));
		return PaymentReceipt.of(transaction, order);
	}

	/**
	 * Works out what {@code partsToPay} equal shares cost right now. Once a part has
	 * been paid the split is locked: the same part count must be used and the
	 * shares are taken from the amount fixed when the split started. Until then the
	 * split can be restarted with a different part count over the outstanding amount.
	 */
	private EqualSharePlan planEqualShares(Order order, int parts, int partsToPay, BigDecimal outstanding) {
		if (outstanding.signum() <= 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has nothing left to pay");
		}
		int paidBefore = order.getEqualSplitPaidParts();
		BigDecimal base = outstanding;
		if (order.getEqualSplitParts() != null && paidBefore > 0) {
			if (order.getEqualSplitParts() != parts) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The bill is already split into " + order.getEqualSplitParts() + " parts");
			}
			base = order.getEqualSplitBase();
		} else {
			paidBefore = 0;
		}
		if (partsToPay < 1 || partsToPay > parts - paidBefore) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Only " + (parts - paidBefore) + " of " + parts + " parts are left to pay");
		}

		int paidAfter = paidBefore + partsToPay;
		boolean finalShare = paidAfter == parts;
		BigDecimal amount = finalShare
			? outstanding
			: splitCalculationService.calculateEqualShare(base, parts, paidAfter)
				.subtract(splitCalculationService.calculateEqualShare(base, parts, paidBefore))
				.min(outstanding);
		return new EqualSharePlan(base, paidBefore, paidAfter, amount, finalShare || amount.compareTo(outstanding) == 0);
	}

	private record EqualSharePlan(
			BigDecimal base, int paidBefore, int paidAfter, BigDecimal amount, boolean settlesOrder) {
	}

	private Order openOrder(RestaurantTable table) {
		return orderRepository
			.findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), OrderStatus.OPEN)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open order for this table"));
	}

	/** The open order, row-locked so payments on the same bill are serialized. */
	private Order openOrderForUpdate(RestaurantTable table) {
		return orderRepository.findOpenOrderForUpdate(table.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No open order for this table"));
	}

	/** What is still owed on the bill. Tips go to the staff, so they never pay the bill down. */
	private BigDecimal outstanding(Order order) {
		BigDecimal paid = transactionRepository
			.findByOrderIdAndStatus(order.getId(), TransactionStatus.SUCCESS).stream()
			.map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return splitCalculationService.calculateFullPayment(order.getTotalAmount(), paid);
	}
}
