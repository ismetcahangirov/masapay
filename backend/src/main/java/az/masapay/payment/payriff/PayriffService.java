package az.masapay.payment.payriff;

import az.masapay.domain.Transaction;
import az.masapay.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Opens a Payriff payment page for a pending transaction (#33). The callback URL carries our own
 * signature of the transaction id (#34), because Payriff does not sign its callbacks.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffService {

	private static final String OPERATION_PURCHASE = "PURCHASE";
	private static final String LANGUAGE = "AZ";

	private final PayriffClient client;
	private final PayriffSignature signature;
	private final TransactionRepository transactionRepository;
	private final String callbackUrl;
	private final String redirectUrl;

	public PayriffService(PayriffClient client, PayriffSignature signature,
			TransactionRepository transactionRepository,
			@Value("${masapay.payriff.callback-url}") String callbackUrl,
			@Value("${masapay.payriff.redirect-url}") String redirectUrl) {
		this.client = client;
		this.signature = signature;
		this.transactionRepository = transactionRepository;
		this.callbackUrl = callbackUrl;
		this.redirectUrl = redirectUrl;
	}

	/**
	 * Creates the Payriff order for a saved transaction and stores Payriff's ids on it. Not
	 * {@code @Transactional}: the Payriff call must not hold a database transaction open.
	 */
	public PayriffCheckout startCheckout(Transaction transaction) {
		String txId = transaction.getId().toString();
		String signedCallback = UriComponentsBuilder.fromUriString(callbackUrl)
				.queryParam("tx", txId)
				.queryParam("sig", signature.sign(txId))
				.toUriString();

		PayriffOrder order = client.createOrder(new PayriffCreateOrderRequest(
				transaction.getTotalAmount(),
				transaction.getCurrency(),
				OPERATION_PURCHASE,
				LANGUAGE,
				"masapay payment " + txId,
				signedCallback,
				redirectUrl));

		transaction.setPayriffOrderId(order.orderId());
		transaction.setPayriffSessionId(order.sessionId());
		if (order.transactionId() != null) {
			transaction.setPayriffPaymentId(order.transactionId().toString());
		}
		transactionRepository.save(transaction);
		return new PayriffCheckout(order.orderId(), order.paymentUrl());
	}
}
