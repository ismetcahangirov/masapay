package az.masapay.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.admin.dto.IntegrationSettings;
import az.masapay.admin.dto.UpdateIntegrationSettingsRequest;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.PosType;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class IntegrationSettingsServiceTest {

	private static final String API_LOGIN = "iiko-login-9876";
	private static final String SECRET_KEY = "sk_live_1234abcd";

	@Mock
	private RestaurantRepository restaurantRepository;

	@Mock
	private UserRepository userRepository;

	private IntegrationSettingsService service;

	private final UUID restaurantId = UUID.randomUUID();
	private final UUID organizationId = UUID.randomUUID();
	private final AuthenticatedUser superAdmin =
		new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", UserRole.SUPER_ADMIN);

	private Restaurant restaurant;

	@BeforeEach
	void setUp() {
		service = new IntegrationSettingsService(restaurantRepository, userRepository);
		restaurant = new Restaurant();
		restaurant.setId(restaurantId);
		restaurant.setPosType(PosType.IIKO);
		restaurant.setIikoApiLogin(API_LOGIN);
		restaurant.setIikoOrganizationId(organizationId);
		restaurant.setPayriffMerchantId("ES1234567");
		restaurant.setPayriffSecretKey(SECRET_KEY);
		lenient().when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
		lenient().when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private AuthenticatedUser managerOf(UUID managedRestaurantId) {
		Restaurant managed = new Restaurant();
		managed.setId(managedRestaurantId);
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setRole(UserRole.RESTO_MANAGER);
		user.setRestaurant(managed);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		return new AuthenticatedUser(user.getId(), "manager@example.com", UserRole.RESTO_MANAGER);
	}

	private static UpdateIntegrationSettingsRequest secretsOnly(String apiLogin, String secretKey) {
		return new UpdateIntegrationSettingsRequest(null, apiLogin, null, null, null, secretKey, null, null);
	}

	@Test
	void readMasksSecretsAndKeepsPlainFields() {
		IntegrationSettings settings = service.getSettings(restaurantId, superAdmin);

		assertThat(settings.payriffSecretKey()).isEqualTo(SecretMasker.MASK + "abcd");
		assertThat(settings.iikoApiLogin()).isEqualTo(SecretMasker.MASK + "9876");
		assertThat(settings.payriffSecretKeySet()).isTrue();
		assertThat(settings.iikoApiLoginSet()).isTrue();
		assertThat(settings.payriffMerchantId()).isEqualTo("ES1234567");
		assertThat(settings.iikoOrganizationId()).isEqualTo(organizationId);
		assertThat(settings.posType()).isEqualTo(PosType.IIKO);
	}

	@Test
	void readWithoutSecretsReportsThemAsNotSet() {
		restaurant.setIikoApiLogin(null);
		restaurant.setPayriffSecretKey(null);

		IntegrationSettings settings = service.getSettings(restaurantId, superAdmin);

		assertThat(settings.payriffSecretKey()).isNull();
		assertThat(settings.payriffSecretKeySet()).isFalse();
		assertThat(settings.iikoApiLoginSet()).isFalse();
	}

	@Test
	void updateChangesOnlyProvidedFields() {
		UUID paymentTypeId = UUID.randomUUID();

		IntegrationSettings result = service.updateSettings(restaurantId,
			new UpdateIntegrationSettingsRequest(null, null, null, paymentTypeId, "ES7654321", "sk_live_new_5678",
				null, null),
			superAdmin);

		assertThat(restaurant.getPosType()).isEqualTo(PosType.IIKO);
		assertThat(restaurant.getIikoApiLogin()).isEqualTo(API_LOGIN);
		assertThat(restaurant.getIikoOrganizationId()).isEqualTo(organizationId);
		assertThat(restaurant.getIikoPaymentTypeId()).isEqualTo(paymentTypeId);
		assertThat(restaurant.getPayriffMerchantId()).isEqualTo("ES7654321");
		assertThat(restaurant.getPayriffSecretKey()).isEqualTo("sk_live_new_5678");
		assertThat(result.payriffSecretKey()).isEqualTo(SecretMasker.MASK + "5678");
	}

	@Test
	void updateSwitchesPosProvider() {
		service.updateSettings(restaurantId,
			new UpdateIntegrationSettingsRequest(PosType.CUSTOM, null, null, null, null, null, null, null), superAdmin);

		assertThat(restaurant.getPosType()).isEqualTo(PosType.CUSTOM);
	}

	@Test
	void maskedPlaceholderKeepsStoredSecrets() {
		service.updateSettings(restaurantId,
			secretsOnly(SecretMasker.mask(API_LOGIN), SecretMasker.mask(SECRET_KEY)), superAdmin);

		assertThat(restaurant.getIikoApiLogin()).isEqualTo(API_LOGIN);
		assertThat(restaurant.getPayriffSecretKey()).isEqualTo(SECRET_KEY);
	}

	@Test
	void blankSecretKeepsStoredSecrets() {
		service.updateSettings(restaurantId, secretsOnly("", "  "), superAdmin);

		assertThat(restaurant.getIikoApiLogin()).isEqualTo(API_LOGIN);
		assertThat(restaurant.getPayriffSecretKey()).isEqualTo(SECRET_KEY);
	}

	@Test
	void blankMerchantIdClearsIt() {
		service.updateSettings(restaurantId,
			new UpdateIntegrationSettingsRequest(null, null, null, null, " ", null, null, null), superAdmin);

		assertThat(restaurant.getPayriffMerchantId()).isNull();
	}

	@Test
	void managerCanManageOwnRestaurantButNotItsPayriffSettings() {
		AuthenticatedUser manager = managerOf(restaurantId);

		IntegrationSettings result = service.updateSettings(restaurantId, secretsOnly("iiko-login-mine", null), manager);

		assertThat(restaurant.getIikoApiLogin()).isEqualTo("iiko-login-mine");
		// Payriff is the platform's: a manager does not see it...
		assertThat(result.payriffMerchantId()).isNull();
		assertThat(result.payriffSecretKey()).isNull();
		assertThat(result.payriffSecretKeySet()).isFalse();
		assertThat(service.getSettings(restaurantId, manager).payriffMerchantId()).isNull();
		// ...and may not change it.
		assertThatThrownBy(() -> service.updateSettings(restaurantId, secretsOnly(null, "sk_live_mine_0000"), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThat(restaurant.getPayriffSecretKey()).isEqualTo(SECRET_KEY);
	}

	@Test
	void managerOfAnotherRestaurantIsForbidden() {
		AuthenticatedUser manager = managerOf(UUID.randomUUID());

		assertThatThrownBy(() -> service.getSettings(restaurantId, manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThatThrownBy(() -> service.updateSettings(restaurantId, secretsOnly(null, "sk_live_x_1111"), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThat(restaurant.getPayriffSecretKey()).isEqualTo(SECRET_KEY);
		verify(restaurantRepository, never()).save(any());
	}

	@Test
	void unknownRestaurantIsNotFound() {
		UUID unknownId = UUID.randomUUID();
		when(restaurantRepository.findById(unknownId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getSettings(unknownId, superAdmin))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void serviceChargeIsSwitchedOnWithAPercentAndOffAgain() {
		service.updateSettings(restaurantId, new UpdateIntegrationSettingsRequest(null, null, null, null, null, null,
				true, new BigDecimal("10.00")), superAdmin);
		assertThat(restaurant.getServiceChargePercent()).isEqualByComparingTo("10.00");

		service.updateSettings(restaurantId, new UpdateIntegrationSettingsRequest(null, null, null, null, null, null,
				false, null), superAdmin);
		assertThat(restaurant.getServiceChargePercent()).isNull();
	}

	@Test
	void switchingServiceChargeOnWithoutAPercentIs400() {
		assertThatThrownBy(() -> service.updateSettings(restaurantId,
				new UpdateIntegrationSettingsRequest(null, null, null, null, null, null, true, null), superAdmin))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}
}
