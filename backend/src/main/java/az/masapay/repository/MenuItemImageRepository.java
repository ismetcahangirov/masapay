package az.masapay.repository;

import az.masapay.domain.MenuItemImage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemImageRepository extends JpaRepository<MenuItemImage, UUID> {

	Optional<MenuItemImage> findByItemId(UUID itemId);
}
