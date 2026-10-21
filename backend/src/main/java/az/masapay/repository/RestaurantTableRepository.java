package az.masapay.repository;

import az.masapay.domain.RestaurantTable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

	/** Resolve a table from the token embedded in its QR code (customer PWA, EPIC 4). */
	Optional<RestaurantTable> findByQrToken(UUID qrToken);

	List<RestaurantTable> findByRestaurantId(UUID restaurantId);

	/** Tables of a restaurant in label order, for the bulk QR export (#47). */
	List<RestaurantTable> findByRestaurantIdOrderByLabelAsc(UUID restaurantId);
}
