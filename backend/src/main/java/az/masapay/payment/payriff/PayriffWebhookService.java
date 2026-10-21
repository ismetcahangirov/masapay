package az.masapay.payment.payriff;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.PaymentSettlementService;
import az.masapay.repository.TransactionRepository;
import java.time.Instant;
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

		Transaction transaction = transactionRepository.findByIdForUpdate(UUID.fromString(txId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		PayriffCallback.Payload payload = callback == null ? null : callback.payload();
		if (payload == null || !Objects.equals(payload.orderId(), transaction.getPayriffOrderId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Callback does not match the transaction");
		}
		if (transaction.getStatus() != TransactionStatus.PENDING) {
			log.info("Duplicate Payriff callback for transaction {} ignored", txId);
			return;
		}

		if (APPROVED.equals(payload.paymentStatus())) {
			transaction.setStatus(TransactionStatus.SUCCESS);
			transaction.setPaidAt(Instant.now());
			transactionRepository.save(transaction);
			settlementService.settleIfFullyPaid(transaction.getOrder());
		} else if (FAILED.contains(payload.paymentStatus())) {
			transaction.setStatus(TransactionStatus.FAILED);
			transactionRepository.save(transaction);
		}
		// Any other status (CREATED, PENDING, IN_REVIEW...) is not final yet: wait for the next callback.
	}
}
