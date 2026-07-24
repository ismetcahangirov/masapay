package az.masapay.admin;

import az.masapay.admin.dto.UpdateUserRequest;
import az.masapay.admin.dto.UserSummary;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Administrative user provisioning: listing accounts and activating/assigning them. */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserAdminService {

	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;

	public UserAdminService(UserRepository userRepository, RestaurantRepository restaurantRepository) {
		this.userRepository = userRepository;
		this.restaurantRepository = restaurantRepository;
	}

	@Transactional(readOnly = true)
	public List<UserSummary> listUsers() {
		return userRepository.findAll().stream().map(UserSummary::from).toList();
	}

	@Transactional
	public UserSummary updateUser(UUID userId, UpdateUserRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (request.enabled() != null) {
			user.setEnabled(request.enabled());
		}
		if (request.role() != null) {
			user.setRole(request.role());
		}
		if (request.restaurantId() != null) {
			Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
			user.setRestaurant(restaurant);
		}
		return UserSummary.from(userRepository.save(user));
	}
}
