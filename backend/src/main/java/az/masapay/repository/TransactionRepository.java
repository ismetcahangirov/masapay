package az.masapay.repository;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	List<Transaction> findByOrderId(UUID orderId);

	List<Transaction> findByOrderIdAndStatus(UUID orderId, TransactionStatus status);

	/** Look up a transaction by its Payriff order id for idempotent webhook handling (#35). */
	Optional<Transaction> findByPayriffOrderId(String payriffOrderId);

	/**
	 * Loads a transaction with a row lock held until the transaction ends (#35). Payriff may send
	 * the same callback twice at once; the lock makes the second one wait and then see the first
	 * one's result, so the payment is applied only once.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT t FROM Transaction t WHERE t.id = :id")
	Optional<Transaction> findByIdForUpdate(@Param("id") UUID id);
}
