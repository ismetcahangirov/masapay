package az.masapay.repository;

import az.masapay.domain.OrderItem;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.KitchenStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

	List<OrderItem> findByOrderId(UUID orderId);

	/**
	 * The kitchen's work for one restaurant (#142), oldest first: everything not ready yet, plus
	 * what became ready since {@code readySince} so the chef still sees what was just finished.
	 * The table comes with each line, for the ticket header. A cancelled order's dishes drop off (#186).
	 */
	@Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o JOIN FETCH o.table "
		+ "WHERE o.restaurant.id = :restaurantId "
		+ "AND o.status <> az.masapay.domain.enums.OrderStatus.CANCELLED AND (oi.kitchenStatus IN :open "
		+ "OR (oi.kitchenStatus = :ready AND oi.updatedAt >= :readySince)) "
		+ "ORDER BY oi.createdAt ASC")
	List<OrderItem> findKitchenWork(@Param("restaurantId") UUID restaurantId,
			@Param("open") Collection<KitchenStatus> open, @Param("ready") KitchenStatus ready,
			@Param("readySince") Instant readySince);

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
