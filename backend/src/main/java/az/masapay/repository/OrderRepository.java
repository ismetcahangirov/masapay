package az.masapay.repository;

import az.masapay.domain.Order;
import az.masapay.domain.enums.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	/** Live orders for a restaurant in a given state (admin floor monitor, EPIC 9). */
	List<Order> findByRestaurantIdAndStatus(UUID restaurantId, OrderStatus status);

	List<Order> findByTableId(UUID tableId);

	/** The current order for a table in a given state (at most one OPEN order per table). */
	Optional<Order> findFirstByTableIdAndStatusOrderByOpenedAtDesc(UUID tableId, OrderStatus status);

	Optional<Order> findByRestaurantIdAndPosOrderId(UUID restaurantId, String posOrderId);
}
