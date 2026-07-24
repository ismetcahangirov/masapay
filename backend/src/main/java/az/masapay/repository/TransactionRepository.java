package az.masapay.repository;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	List<Transaction> findByOrderId(UUID orderId);

	List<Transaction> findByOrderIdAndStatus(UUID orderId, TransactionStatus status);

	/** Look up a transaction by its Payriff order id for idempotent webhook handling (#35). */
	Optional<Transaction> findByPayriffOrderId(String payriffOrderId);
}
