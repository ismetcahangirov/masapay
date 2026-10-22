package az.masapay.repository;

import az.masapay.domain.MenuComboComponent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuComboComponentRepository extends JpaRepository<MenuComboComponent, UUID> {

	/** True when the dish is inside some combo, so it cannot be deleted (#138). */
	boolean existsByComponentId(UUID componentId);
}
