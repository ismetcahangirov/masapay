package az.masapay.admin;

import az.masapay.admin.dto.RestaurantSummary;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Restaurants the caller may manage: every restaurant for a SUPER_ADMIN, only their own for a
 * RESTO_MANAGER. The admin panel uses it to know which restaurant's settings to open (#46).
 */
@RestController
@RequestMapping("/api/admin/restaurants")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminRestaurantController {

	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;

	public AdminRestaurantController(RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<RestaurantSummary> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		List<Restaurant> restaurants = principal.role() == UserRole.SUPER_ADMIN
			? restaurantRepository.findAll()
			: userRepository.findById(principal.id()).map(User::getRestaurant).map(List::of).orElse(List.of());
		return restaurants.stream()
			.sorted(Comparator.comparing(Restaurant::getName))
			.map(RestaurantSummary::from)
			.toList();
	}
}
