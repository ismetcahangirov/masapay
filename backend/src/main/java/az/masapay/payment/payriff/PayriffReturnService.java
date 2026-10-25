package az.masapay.payment.payriff;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.repository.TransactionRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * The guest's way back from Payriff's payment page. Payriff sends the browser to our callback URL
 * with a GET after 3-D Secure (its {@code redirectUrl} is not used), separately from the callback
 * it posts. We check our signature, ask Payriff for the order's status, so the payment is settled
 * even if the posted callback is late or lost, and send the guest on to the result page.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffReturnService {

	private static final Logger log = LoggerFactory.getLogger(PayriffReturnService.class);

	private final PayriffSignature signature;
	private final TransactionRepository transactionRepository;
	private final PayriffClient client;
	private final PayriffWebhookService webhookService;
	private final PayriffService payriffService;

	public PayriffReturnService(PayriffSignature signature, TransactionRepository transactionRepository,
			PayriffClient client, PayriffWebhookService webhookService, PayriffService payriffService) {
		this.signature = signature;
		this.transactionRepository = transactionRepository;
		this.client = client;
		this.webhookService = webhookService;
		this.payriffService = payriffService;
	}

	/** Where to send the guest: our result page for this payment. */
	public String guestReturned(String txId, String sig) {
		signature.verify(txId, sig);
		UUID id = UUID.fromString(txId);
		Transaction transaction = transactionRepository.findById(id).orElse(null);
		if (transaction != null && transaction.getStatus() == TransactionStatus.PENDING
				&& transaction.getPayriffOrderId() != null) {
			// Outside a database transaction: the call to Payriff must not hold a row lock.
			try {
				PayriffOrderInfo order = client.getOrder(transaction.getPayriffOrderId());
				webhookService.apply(id, order.orderId(), order.paymentStatus());
			}
			catch (RuntimeException e) {
				// The result page keeps polling; the posted callback will still settle it.
				log.warn("Could not check payment {} with Payriff on the guest's return: {}", txId, e.getMessage());
			}
		}
		return payriffService.resultPage(txId);
	}
}
