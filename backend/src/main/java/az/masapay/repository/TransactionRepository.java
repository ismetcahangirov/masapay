package az.masapay.repository;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.tip.dto.WaiterTip;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	List<Transaction> findByOrderId(UUID orderId);

	List<Transaction> findByOrderIdAndStatus(UUID orderId, TransactionStatus status);

	/** Look up a transaction by its Payriff order id for idempotent webhook handling (#35). */
	Optional<Transaction> findByPayriffOrderId(String payriffOrderId);

	/**
	 * Tips per waiter (#48): successful payments with a tip, paid in {@code [from, to)}.
	 * Orders without a waiter form one row whose waiter fields are null.
	 */
	@Query("""
		select new az.masapay.tip.dto.WaiterTip(w.id, w.name, w.email, count(t), sum(t.tipAmount))
		from Transaction t
		join t.order o
		left join o.waiter w
		where t.restaurant.id = :restaurantId
			and t.status = :status
			and t.tipAmount > 0
			and t.paidAt >= :from
			and t.paidAt < :to
		group by w.id, w.name, w.email
		order by sum(t.tipAmount) desc
		""")
	List<WaiterTip> sumTipsByWaiter(
		@Param("restaurantId") UUID restaurantId,
		@Param("status") TransactionStatus status,
		@Param("from") Instant from,
		@Param("to") Instant to);
}
