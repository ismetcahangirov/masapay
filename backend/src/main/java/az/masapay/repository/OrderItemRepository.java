package az.masapay.repository;

import az.masapay.domain.OrderItem;
import az.masapay.domain.Transaction;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

	List<OrderItem> findByOrderId(UUID orderId);

	boolean existsByOrderIdAndPaidFalse(UUID orderId);

	/**
	 * Atomically claims the given still-unpaid items for a transaction. The
	 * {@code paid = false} predicate makes this race-safe: if another payer has
	 * already claimed one of the items, that row is not updated, so the returned
	 * count is less than the number requested and the caller can abort.
	 *
	 * @return the number of items actually claimed
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE OrderItem oi SET oi.paid = true, oi.paidTransaction = :transaction "
		+ "WHERE oi.id IN :ids AND oi.order.id = :orderId AND oi.paid = false")
	int claimItems(@Param("ids") Collection<UUID> ids, @Param("orderId") UUID orderId,
			@Param("transaction") Transaction transaction);
}
