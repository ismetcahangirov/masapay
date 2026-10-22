package az.masapay.repository;

import az.masapay.domain.MenuCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

	/** Categories in the manager's order; ties fall back to the name. */
	List<MenuCategory> findByRestaurantIdOrderBySortOrderAscNameAsc(UUID restaurantId);

	boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);

	boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(UUID restaurantId, String name, UUID id);
}
