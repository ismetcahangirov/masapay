package az.masapay.payment.refund;

import az.masapay.domain.Transaction;
import az.masapay.domain.User;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.payment.payriff.PayriffClient;
import az.masapay.payment.payriff.PayriffRefundRequest;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * One-click full refund of a Payriff payment from the admin panel (#36). The transaction row is
 * locked for the whole call, so a double click cannot send the money back twice.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RefundService {

	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final PayriffClient payriffClient;

	public RefundService(TransactionRepository transactionRepository, UserRepository userRepository,
			PayriffClient payriffClient) {
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.payriffClient = payriffClient;
	}

	@Transactional
	public RefundResult refund(UUID transactionId, AuthenticatedUser admin, String reason) {
		Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		checkOwnRestaurant(admin, transaction);

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

		payriffClient.refund(new PayriffRefundRequest(
				transaction.getPayriffOrderId(), transaction.getTotalAmount(), reason));

		transaction.setStatus(TransactionStatus.REFUNDED);
		transaction.setRefundedAmount(transaction.getTotalAmount());
		transactionRepository.save(transaction);
		return RefundResult.of(transaction);
	}

	/** A restaurant manager may refund only their own restaurant's payments; a super admin any. */
	private void checkOwnRestaurant(AuthenticatedUser admin, Transaction transaction) {
		if (admin.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		User user = userRepository.findById(admin.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		UUID restaurantId = transaction.getRestaurant().getId();
		if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(restaurantId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
	}
}
