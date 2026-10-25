package az.masapay.payment.payriff;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.PaymentSettlementService;
import az.masapay.repository.TransactionRepository;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Applies a Payriff callback to its transaction (#35). The signature is checked first (#34);
 * a callback for a transaction that is no longer PENDING is a duplicate and changes nothing.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffWebhookService {

	private static final Logger log = LoggerFactory.getLogger(PayriffWebhookService.class);

	private static final String APPROVED = "APPROVED";
	private static final Set<String> FAILED = Set.of("DECLINED", "CANCELED", "EXPIRED");

	private final PayriffSignature signature;
	private final TransactionRepository transactionRepository;
	private final PaymentSettlementService settlementService;

	public PayriffWebhookService(PayriffSignature signature, TransactionRepository transactionRepository,
			PaymentSettlementService settlementService) {
		this.signature = signature;
		this.transactionRepository = transactionRepository;
		this.settlementService = settlementService;
	}

	@Transactional
	public void handle(String txId, String sig, PayriffCallback callback) {
		signature.verify(txId, sig);
		PayriffCallback.Payload payload = callback == null ? null : callback.payload();
		if (payload == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Callback does not match the transaction");
		}
		apply(UUID.fromString(txId), payload.orderId(), payload.paymentStatus());
	}

	/**
	 * Applies Payriff's status of {@code orderId} to its transaction, from a callback or from asking
	 * Payriff when the guest comes back. Idempotent: a transaction no longer PENDING is left alone.
	 */
	@Transactional
	public void apply(UUID txId, String orderId, String paymentStatus) {
		Transaction transaction = transactionRepository.findByIdForUpdate(txId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		if (orderId == null || !Objects.equals(orderId, transaction.getPayriffOrderId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Callback does not match the transaction");
		}
		if (transaction.getStatus() != TransactionStatus.PENDING) {
			log.info("Payriff status for transaction {} already applied, ignored", txId);
			return;
		}

		if (APPROVED.equals(paymentStatus)) {
			// Applies the split the guest chose (items, equal parts or the whole bill) and marks it SUCCESS.
			settlementService.settlePending(transaction);
		} else if (FAILED.contains(paymentStatus)) {
			transaction.setStatus(TransactionStatus.FAILED);
			transactionRepository.save(transaction);
		}
		// Any other status (CREATED, PENDING, IN_REVIEW...) is not final yet: wait for the next callback.
	}
}
