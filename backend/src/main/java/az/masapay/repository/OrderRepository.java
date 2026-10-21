package az.masapay.repository;

import az.masapay.domain.Order;
import az.masapay.domain.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	/** Live orders for a restaurant in a given state (admin floor monitor, EPIC 9). */
	List<Order> findByRestaurantIdAndStatus(UUID restaurantId, OrderStatus status);

	List<Order> findByTableId(UUID tableId);

	/** The current order for a table in a given state (at most one OPEN order per table). */
	Optional<Order> findFirstByTableIdAndStatusOrderByOpenedAtDesc(UUID tableId, OrderStatus status);

	/** The table's latest bill in this status that was closed after {@code since} (#24 receipt view). */
	Optional<Order> findFirstByTableIdAndStatusAndClosedAtAfterOrderByClosedAtDesc(UUID tableId, OrderStatus status,
			Instant since);

	/**
	 * The table's OPEN order, row-locked ({@code SELECT ... FOR UPDATE}) until the
	 * transaction ends. Every payment loads the order through this, so concurrent
	 * payments on one bill run one after another and each sees what the previous
	 * one paid (two simultaneous "pay full" calls can no longer both charge the bill).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM Order o WHERE o.table.id = :tableId "
		+ "AND o.status = az.masapay.domain.enums.OrderStatus.OPEN ORDER BY o.openedAt DESC LIMIT 1")
	Optional<Order> findOpenOrderForUpdate(@Param("tableId") UUID tableId);

	Optional<Order> findByRestaurantIdAndPosOrderId(UUID restaurantId, String posOrderId);

	/**
	 * Atomically records paid parts of an equal split (#26) as a compare-and-set on
	 * the paid-part counter: the row is only updated if no other payer has claimed
	 * a part since {@code expectedPaidParts} was read. When nothing is paid yet the
	 * same call also (re)fixes the part count and the amount being split.
	 *
	 * @return 1 if the parts were claimed, 0 if another payer got there first
	 */
	@Modifying(flushAutomatically = true)
	@Query("UPDATE Order o SET o.equalSplitParts = :parts, o.equalSplitBase = :base, "
		+ "o.equalSplitPaidParts = :newPaidParts "
		+ "WHERE o.id = :orderId AND o.status = az.masapay.domain.enums.OrderStatus.OPEN "
		+ "AND o.equalSplitPaidParts = :expectedPaidParts")
	int claimEqualParts(@Param("orderId") UUID orderId, @Param("parts") int parts, @Param("base") BigDecimal base,
			@Param("expectedPaidParts") int expectedPaidParts, @Param("newPaidParts") int newPaidParts);
}
