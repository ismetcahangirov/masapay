package az.masapay.repository;

import az.masapay.dashboard.dto.PaidAmount;
import az.masapay.dashboard.dto.PaymentTotals;
import az.masapay.dashboard.dto.StatusCount;
import az.masapay.platform.dto.RestaurantPayments;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.tip.dto.WaiterTip;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
	extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

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

	/** Dashboard (#43): bill amount and tips of successful payments paid in {@code [from, to)}. */
	@Query("""
		select new az.masapay.dashboard.dto.PaymentTotals(sum(t.amount), sum(t.tipAmount))
		from Transaction t
		where t.restaurant.id = :restaurantId
			and t.status = az.masapay.domain.enums.TransactionStatus.SUCCESS
			and t.paidAt >= :from
			and t.paidAt < :to
		""")
	PaymentTotals sumSuccessfulPayments(
		@Param("restaurantId") UUID restaurantId,
		@Param("from") Instant from,
		@Param("to") Instant to);

	/**
	 * Dashboard (#43): successful payments paid in {@code [from, to)}, one row each. The
	 * caller groups them by Asia/Baku day, which keeps the query free of database-specific
	 * date and time-zone functions.
	 */
	@Query("""
		select new az.masapay.dashboard.dto.PaidAmount(t.paidAt, t.amount, t.tipAmount)
		from Transaction t
		where t.restaurant.id = :restaurantId
			and t.status = az.masapay.domain.enums.TransactionStatus.SUCCESS
			and t.paidAt >= :from
			and t.paidAt < :to
		""")
	List<PaidAmount> findSuccessfulPayments(
		@Param("restaurantId") UUID restaurantId,
		@Param("from") Instant from,
		@Param("to") Instant to);

	/** Dashboard (#43): Payriff payments started in {@code [from, to)}, counted per status. */
	@Query("""
		select new az.masapay.dashboard.dto.StatusCount(t.status, count(t))
		from Transaction t
		where t.restaurant.id = :restaurantId
			and t.payriffOrderId is not null
			and t.createdAt >= :from
			and t.createdAt < :to
		group by t.status
		""")
	List<StatusCount> countPayriffByStatus(
		@Param("restaurantId") UUID restaurantId,
		@Param("from") Instant from,
		@Param("to") Instant to);

	/** Platform analytics (#149): successful payments paid in {@code [from, to)}, per restaurant. */
	@Query("""
		select new az.masapay.platform.dto.RestaurantPayments(t.restaurant.id, count(t), sum(t.amount),
			sum(t.tipAmount))
		from Transaction t
		where t.status = az.masapay.domain.enums.TransactionStatus.SUCCESS
			and t.paidAt >= :from
			and t.paidAt < :to
		group by t.restaurant.id
		""")
	List<RestaurantPayments> sumSuccessfulPaymentsByRestaurant(
		@Param("from") Instant from,
		@Param("to") Instant to);
}
