package az.masapay.repository;

import az.masapay.domain.MenuItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

	List<MenuItem> findByRestaurantIdOrderByNameAsc(UUID restaurantId);

	boolean existsByCategoryId(UUID categoryId);
}
