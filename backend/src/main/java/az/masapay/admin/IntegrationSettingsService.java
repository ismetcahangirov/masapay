package az.masapay.admin;

import az.masapay.admin.dto.IntegrationSettings;
import az.masapay.admin.dto.UpdateIntegrationSettingsRequest;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reads and updates a restaurant's POS and Payriff settings. A SUPER_ADMIN may
 * manage any restaurant; a RESTO_MANAGER only the restaurant they belong to.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationSettingsService {

	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;

	public IntegrationSettingsService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public IntegrationSettings getSettings(UUID restaurantId, AuthenticatedUser principal) {
		return IntegrationSettings.from(loadAuthorized(restaurantId, principal));
	}

	@Transactional
	public IntegrationSettings updateSettings(
			UUID restaurantId, UpdateIntegrationSettingsRequest request, AuthenticatedUser principal) {
		Restaurant restaurant = loadAuthorized(restaurantId, principal);

		if (request.posType() != null) {
			restaurant.setPosType(request.posType());
		}
		if (!SecretMasker.isUnchanged(request.iikoApiLogin(), restaurant.getIikoApiLogin())) {
			restaurant.setIikoApiLogin(request.iikoApiLogin().strip());
		}
		if (request.iikoOrganizationId() != null) {
			restaurant.setIikoOrganizationId(request.iikoOrganizationId());
		}
		if (request.iikoPaymentTypeId() != null) {
			restaurant.setIikoPaymentTypeId(request.iikoPaymentTypeId());
		}
		if (request.payriffMerchantId() != null) {
			String merchantId = request.payriffMerchantId().strip();
			restaurant.setPayriffMerchantId(merchantId.isEmpty() ? null : merchantId);
		}
		if (!SecretMasker.isUnchanged(request.payriffSecretKey(), restaurant.getPayriffSecretKey())) {
			restaurant.setPayriffSecretKey(request.payriffSecretKey().strip());
		}
		return IntegrationSettings.from(restaurantRepository.save(restaurant));
	}

	/**
	 * Checks the caller may manage the restaurant (403 otherwise), then loads it
	 * (404 if unknown). Ownership is checked first so a manager cannot probe which
	 * restaurant ids exist.
	 */
	private Restaurant loadAuthorized(UUID restaurantId, AuthenticatedUser principal) {
		if (principal.role() != UserRole.SUPER_ADMIN && !belongsTo(principal, restaurantId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to manage this restaurant");
		}
		return restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
	}

	private boolean belongsTo(AuthenticatedUser principal, UUID restaurantId) {
		if (principal.role() != UserRole.RESTO_MANAGER) {
			return false;
		}
		return userRepository.findById(principal.id())
			.map(User::getRestaurant)
			.map(Restaurant::getId)
			.filter(restaurantId::equals)
			.isPresent();
	}
}
