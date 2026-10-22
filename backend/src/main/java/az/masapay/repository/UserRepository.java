package az.masapay.repository;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByGoogleSub(String googleSub);

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailIgnoreCase(String email);

	/** A restaurant's staff in the given roles, by name then email (#145). */
	List<User> findByRestaurantIdAndRoleInOrderByNameAscEmailAsc(UUID restaurantId, Collection<UserRole> roles);
}
