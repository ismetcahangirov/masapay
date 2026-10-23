package az.masapay.admin;

import az.masapay.admin.dto.RestaurantSummary;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.restaurant.RestaurantGate;
import az.masapay.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Restaurants the caller may manage: every restaurant for a SUPER_ADMIN, only their own for a
 * RESTO_MANAGER or WAITER. The admin panel uses it to know which restaurant to open (#46, tables).
 */
@RestController
@RequestMapping("/api/admin/restaurants")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER','WAITER','CHEF')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminRestaurantController {

	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public AdminRestaurantController(RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this(restaurantRepository, userRepository, Clock.systemUTC());
	}

	AdminRestaurantController(RestaurantRepository restaurantRepository, UserRepository userRepository, Clock clock) {
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<RestaurantSummary> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		List<Restaurant> restaurants = principal.role() == UserRole.SUPER_ADMIN
			? restaurantRepository.findAll()
			: userRepository.findById(principal.id()).map(User::getRestaurant).map(List::of).orElse(List.of());
		LocalDate today = LocalDate.now(clock.withZone(RestaurantGate.ZONE));
		return restaurants.stream()
			.sorted(Comparator.comparing(Restaurant::getName))
			.map(restaurant -> RestaurantSummary.from(restaurant, today))
			.toList();
	}
}
