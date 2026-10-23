package az.masapay.platform;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.platform.dto.CreateRestaurantRequest;
import az.masapay.platform.dto.PlatformRestaurantView;
import az.masapay.platform.dto.SubscriptionRequest;
import az.masapay.platform.dto.UpdateRestaurantRequest;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.restaurant.RestaurantGate;
import az.masapay.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The super admin's restaurants (#147): open a restaurant with its first manager, add managers,
 * rename it, switch it on or off. A manager is named by the email of their Google account and is
 * ready at once: the first Google sign-in with that email lands straight in the restaurant. The
 * super admin vouches for them, unlike waiters and chefs, whom a manager approves (#145).
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformRestaurantService {

	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public PlatformRestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this(restaurantRepository, userRepository, Clock.systemUTC());
	}

	PlatformRestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository,
			Clock clock) {
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<PlatformRestaurantView> list(AuthenticatedUser caller) {
		checkSuperAdmin(caller);
		return restaurantRepository.findAll().stream()
			.sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
			.map(this::view)
			.toList();
	}

	/** Checks the manager's email first, so a refused email leaves no half-made restaurant. */
	@Transactional
	public PlatformRestaurantView create(CreateRestaurantRequest request, AuthenticatedUser caller) {
		checkSuperAdmin(caller);
		User manager = freeAccount(request.managerEmail());

		Restaurant restaurant = new Restaurant();
		restaurant.setName(request.name().trim());
		restaurant.setCurrency(request.currency());
		restaurant.setPosType(request.posType());
		restaurant = restaurantRepository.save(restaurant);

		makeManager(manager, restaurant);
		return view(restaurant);
	}

	@Transactional
	public PlatformRestaurantView addManager(UUID restaurantId, String email, AuthenticatedUser caller) {
		checkSuperAdmin(caller);
		Restaurant restaurant = restaurant(restaurantId);
		makeManager(freeAccount(email), restaurant);
		return view(restaurant);
	}

	@Transactional
	public PlatformRestaurantView update(UUID restaurantId, UpdateRestaurantRequest request, AuthenticatedUser caller) {
		checkSuperAdmin(caller);
		Restaurant restaurant = restaurant(restaurantId);
		if (request.name() != null) {
			if (request.name().isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The name cannot be blank");
			}
			restaurant.setName(request.name().trim());
		}
		if (request.active() != null) {
			restaurant.setActive(request.active());
		}
		return view(restaurantRepository.save(restaurant));
	}

	/**
	 * Sets or extends a restaurant's subscription (#148). Extending is simply a later end date;
	 * once it is in the future again, its customers can pay online again.
	 */
	@Transactional
	public PlatformRestaurantView setSubscription(UUID restaurantId, SubscriptionRequest request,
			AuthenticatedUser caller) {
		checkSuperAdmin(caller);
		if (request.endsOn().isBefore(request.startsOn())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The last day is before the first day");
		}
		Restaurant restaurant = restaurant(restaurantId);
		restaurant.setSubscriptionPlan(request.plan());
		restaurant.setSubscriptionTrial(request.trial());
		restaurant.setSubscriptionStartsOn(request.startsOn());
		restaurant.setSubscriptionEndsOn(request.endsOn());
		restaurant.setSubscriptionMonthlyFee(request.monthlyFee());
		return view(restaurantRepository.save(restaurant));
	}

	private LocalDate today() {
		return LocalDate.now(clock.withZone(RestaurantGate.ZONE));
	}

	/**
	 * The account for this email: a new one, or someone who signed in but has no role or
	 * restaurant yet. Anyone already working somewhere, or holding a role, is refused.
	 */
	private User freeAccount(String rawEmail) {
		String email = rawEmail.trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
			User invited = new User();
			invited.setEmail(email);
			return invited;
		});
		if (user.getRestaurant() != null || user.getRole() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This account already has a role or a restaurant");
		}
		return user;
	}

	private void makeManager(User user, Restaurant restaurant) {
		user.setRestaurant(restaurant);
		user.setRole(UserRole.RESTO_MANAGER);
		user.setEnabled(true);
		user.setDeactivatedAt(null);
		userRepository.save(user);
	}

	private PlatformRestaurantView view(Restaurant restaurant) {
		return PlatformRestaurantView.of(restaurant, userRepository
			.findByRestaurantIdAndRoleInOrderByNameAscEmailAsc(restaurant.getId(), Set.of(UserRole.RESTO_MANAGER)),
			today());
	}

	private Restaurant restaurant(UUID restaurantId) {
		return restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
	}

	private static void checkSuperAdmin(AuthenticatedUser caller) {
		if (caller.role() != UserRole.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super admins only");
		}
	}
}
