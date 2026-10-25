package az.masapay.payment.refund;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.payment.payriff.PayriffClient;
import az.masapay.payment.payriff.PayriffException;
import az.masapay.payment.payriff.PayriffRefundRequest;
import az.masapay.repository.TransactionRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * One-click full refund of a Payriff payment from the admin panel (#36). The transaction row is
 * locked for the whole call, so a double click cannot send the money back twice. Only a
 * SUPER_ADMIN refunds: the Payriff account is the platform's, not the restaurant's.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RefundService {

	/** Reasons sent with a refused refund, so the admin panel can say why. */
	public static final String REFUND_WINDOW_EXPIRED = "REFUND_WINDOW_EXPIRED";
	public static final String PAYRIFF_REFUSED = "PAYRIFF_REFUSED";
	public static final String PAYRIFF_UNAVAILABLE = "PAYRIFF_UNAVAILABLE";

	private final TransactionRepository transactionRepository;
	private final PayriffClient payriffClient;

	public RefundService(TransactionRepository transactionRepository, PayriffClient payriffClient) {
		this.transactionRepository = transactionRepository;
		this.payriffClient = payriffClient;
	}

	@Transactional
	public RefundResult refund(UUID transactionId, AuthenticatedUser admin, String reason) {
		if (admin.role() != UserRole.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refunds are made by the platform");
		}
		Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

		if (transaction.getStatus() == TransactionStatus.REFUNDED) {
			// Already done (e.g. a double click that waited for the lock): nothing to send again.
			return RefundResult.of(transaction);
		}
		if (transaction.getStatus() != TransactionStatus.SUCCESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a successful payment can be refunded");
		}
		if (transaction.getPayriffOrderId() == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment did not go through Payriff");
		}

		try {
			payriffClient.refund(new PayriffRefundRequest(
					transaction.getPayriffOrderId(), transaction.getTotalAmount(), reason));
		}
		catch (PayriffException e) {
			throw refusal(e);
		}

		transaction.setStatus(TransactionStatus.REFUNDED);
		transaction.setRefundedAmount(transaction.getTotalAmount());
		transactionRepository.save(transaction);
		return RefundResult.of(transaction);
	}

	/**
	 * What the super admin is told when Payriff does not refund: 409 with why when Payriff refused
	 * (past the 30-day window: {@value #REFUND_WINDOW_EXPIRED}), 502 when it could not be reached.
	 * The payment stays SUCCESS either way.
	 */
	static ResponseStatusException refusal(PayriffException e) {
		if (!e.refused()) {
			return new ResponseStatusException(HttpStatus.BAD_GATEWAY, PAYRIFF_UNAVAILABLE);
		}
		String message = e.payriffMessage() == null ? "" : e.payriffMessage();
		return new ResponseStatusException(HttpStatus.CONFLICT,
			message.toLowerCase(Locale.ROOT).contains("refund time expired") ? REFUND_WINDOW_EXPIRED
				: PAYRIFF_REFUSED);
	}
}
