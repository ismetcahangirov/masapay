package az.masapay.payment.payriff;

import az.masapay.domain.Transaction;
import az.masapay.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Opens a Payriff payment page for a pending transaction (#33). The callback URL carries our own
 * signature of the transaction id (#34), because Payriff does not sign its callbacks.
 * <p>
 * With {@code masapay.payriff.mock=true} (LOCAL DEVELOPMENT ONLY) no Payriff call is made: the
 * guest is sent to the frontend's mock payment page instead, which posts the same signed callback
 * our webhook expects, so the whole flow can be tried without Payriff keys (#37).
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffService {

	private static final Logger log = LoggerFactory.getLogger(PayriffService.class);

	private static final String OPERATION_PURCHASE = "PURCHASE";
	private static final String LANGUAGE = "AZ";
	static final String MOCK_ORDER_PREFIX = "MOCK-";

	private final PayriffClient client;
	private final PayriffSignature signature;
	private final TransactionRepository transactionRepository;
	private final String callbackUrl;
	private final String redirectUrl;
	private final boolean mock;
	private final String mockPageUrl;

	public PayriffService(PayriffClient client, PayriffSignature signature,
			TransactionRepository transactionRepository,
			@Value("${masapay.payriff.callback-url}") String callbackUrl,
			@Value("${masapay.payriff.redirect-url}") String redirectUrl,
			@Value("${masapay.payriff.mock:false}") boolean mock,
			@Value("${masapay.payriff.mock-page-url:http://localhost:5173/payment/mock}") String mockPageUrl) {
		this.client = client;
		this.signature = signature;
		this.transactionRepository = transactionRepository;
		this.callbackUrl = callbackUrl;
		this.redirectUrl = redirectUrl;
		this.mock = mock;
		this.mockPageUrl = mockPageUrl;
	}

	@PostConstruct
	void warnIfMock() {
		if (mock) {
			log.warn("PAYRIFF MOCK IS ENABLED: payments are approved without Payriff. Local development only.");
		}
	}

	/**
	 * Creates the Payriff order for a saved transaction and stores Payriff's ids on it. Not
	 * {@code @Transactional}: the Payriff call must not hold a database transaction open.
	 */
	public PayriffCheckout startCheckout(Transaction transaction) {
		String txId = transaction.getId().toString();
		String sig = signature.sign(txId);
		String signedCallback = UriComponentsBuilder.fromUriString(callbackUrl)
				.queryParam("tx", txId)
				.queryParam("sig", sig)
				.toUriString();
		// Payriff sends the guest back here; the result page polls the payment by its id.
		String returnUrl = UriComponentsBuilder.fromUriString(redirectUrl)
				.queryParam("tx", txId)
				.toUriString();

		if (mock) {
			return startMockCheckout(transaction, txId, sig);
		}

		PayriffOrder order = client.createOrder(new PayriffCreateOrderRequest(
				transaction.getTotalAmount(),
				transaction.getCurrency(),
				OPERATION_PURCHASE,
				LANGUAGE,
				"masapay payment " + txId,
				signedCallback,
				returnUrl));

		transaction.setPayriffOrderId(order.orderId());
		transaction.setPayriffSessionId(order.sessionId());
		if (order.transactionId() != null) {
			transaction.setPayriffPaymentId(order.transactionId().toString());
		}
		transactionRepository.save(transaction);
		return new PayriffCheckout(order.orderId(), order.paymentUrl());
	}

	private PayriffCheckout startMockCheckout(Transaction transaction, String txId, String sig) {
		String orderId = MOCK_ORDER_PREFIX + txId;
		transaction.setPayriffOrderId(orderId);
		transactionRepository.save(transaction);
		String page = UriComponentsBuilder.fromUriString(mockPageUrl)
				.queryParam("tx", txId)
				.queryParam("sig", sig)
				.queryParam("order", orderId)
				.queryParam("amount", transaction.getTotalAmount().toPlainString())
				.queryParam("currency", transaction.getCurrency())
				.toUriString();
		return new PayriffCheckout(orderId, page);
	}
}
