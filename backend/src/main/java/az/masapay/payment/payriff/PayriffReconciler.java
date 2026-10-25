package az.masapay.payment.payriff;

import az.masapay.domain.Transaction;
import az.masapay.repository.TransactionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Settles payments Payriff never called back about. A guest who opens the payment page and leaves
 * without paying gets no callback when the order expires, and a callback can also be lost; such a
 * payment would stay PENDING for ever (and keep the bill from being cancelled, #186). Every
 * {@code interval}, payments PENDING for longer than {@code min-age} are checked with Payriff:
 * <ul>
 * <li>approved there: SUCCESS, as the callback would have done;</li>
 * <li>expired, cancelled or declined there, or unknown to Payriff: FAILED;</li>
 * <li>still open there: left alone, unless it is older than {@code give-up-after}, then FAILED;</li>
 * <li>Payriff not answering: left alone until the next round.</li>
 * </ul>
 * The same idempotent logic as the callback is used, so a callback arriving at the same moment does
 * no harm. Mock payments (local development) are never checked.
 */
@Component
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffReconciler {

	private static final Logger log = LoggerFactory.getLogger(PayriffReconciler.class);

	/** Payriff statuses we treat as failed when we give up on an order. */
	static final String EXPIRED = "EXPIRED";
	/** At most this many payments per round, oldest first. */
	static final int BATCH = 50;

	private final TransactionRepository transactionRepository;
	private final PayriffClient client;
	private final PayriffWebhookService webhookService;
	private final Clock clock;
	private final boolean enabled;
	private final Duration minAge;
	private final Duration giveUpAfter;

	@Autowired
	public PayriffReconciler(TransactionRepository transactionRepository, PayriffClient client,
			PayriffWebhookService webhookService,
			@Value("${masapay.payriff.reconcile.enabled:true}") boolean enabled,
			@Value("${masapay.payriff.mock:false}") boolean mock,
			@Value("${masapay.payriff.reconcile.min-age:PT10M}") Duration minAge,
			@Value("${masapay.payriff.reconcile.give-up-after:P2D}") Duration giveUpAfter) {
		this(transactionRepository, client, webhookService, Clock.systemUTC(), enabled && !mock, minAge,
			giveUpAfter);
	}

	PayriffReconciler(TransactionRepository transactionRepository, PayriffClient client,
			PayriffWebhookService webhookService, Clock clock, boolean enabled, Duration minAge,
			Duration giveUpAfter) {
		this.transactionRepository = transactionRepository;
		this.client = client;
		this.webhookService = webhookService;
		this.clock = clock;
		this.enabled = enabled;
		this.minAge = minAge;
		this.giveUpAfter = giveUpAfter;
	}

	@Scheduled(fixedDelayString = "${masapay.payriff.reconcile.interval:PT1M}",
		initialDelayString = "${masapay.payriff.reconcile.initial-delay:PT30S}")
	public void reconcile() {
		if (!enabled) {
			return;
		}
		Instant now = clock.instant();
		List<Transaction> pending = transactionRepository.findStalePayriffPayments(now.minus(minAge),
			PageRequest.of(0, BATCH));
		int settled = 0;
		for (Transaction transaction : pending) {
			if (check(transaction, now)) {
				settled++;
			}
		}
		if (settled > 0) {
			log.info("Payriff reconciliation settled {} of {} pending payment(s)", settled, pending.size());
		}
	}

	/** True when the payment left PENDING (or was given up on) in this round. */
	private boolean check(Transaction transaction, Instant now) {
		String orderId = transaction.getPayriffOrderId();
		boolean tooOld = transaction.getCreatedAt() != null
			&& transaction.getCreatedAt().isBefore(now.minus(giveUpAfter));
		try {
			PayriffOrderInfo order = client.getOrder(orderId);
			webhookService.apply(transaction.getId(), orderId, order.paymentStatus());
			if (PayriffWebhookService.isFinal(order.paymentStatus())) {
				return true;
			}
		}
		catch (PayriffException e) {
			if (!e.refused()) {
				// Payriff not answering: try again next round, never give up because of an outage.
				log.warn("Payriff reconciliation could not check payment {}: {}", transaction.getId(), e.getMessage());
				return false;
			}
			// Payriff does not know the order (it was never really opened): it can not be paid.
			webhookService.apply(transaction.getId(), orderId, EXPIRED);
			return true;
		}
		if (tooOld) {
			log.info("Payment {} still open at Payriff after {}, marked failed", transaction.getId(), giveUpAfter);
			webhookService.apply(transaction.getId(), orderId, EXPIRED);
			return true;
		}
		return false;
	}
}
