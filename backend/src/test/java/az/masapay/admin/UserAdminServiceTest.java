package az.masapay.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import az.masapay.admin.dto.UpdateUserRequest;
import az.masapay.admin.dto.UserSummary;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RestaurantRepository restaurantRepository;

	private UserAdminService service;

	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new UserAdminService(userRepository, restaurantRepository);
	}

	private User pendingUser() {
		User user = new User();
		user.setId(userId);
		user.setEmail("staff@example.com");
		return user;
	}

	@Test
	void listUsersMapsToSummaries() {
		when(userRepository.findAll()).thenReturn(List.of(pendingUser()));

		List<UserSummary> summaries = service.listUsers();

		assertThat(summaries).singleElement()
			.satisfies(s -> {
				assertThat(s.id()).isEqualTo(userId);
				assertThat(s.enabled()).isFalse();
			});
	}

	@Test
	void updateActivatesAndAssignsRoleAndRestaurant() {
		UUID restaurantId = UUID.randomUUID();
		Restaurant restaurant = new Restaurant();
		restaurant.setId(restaurantId);
		User user = pendingUser();
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UserSummary result = service.updateUser(userId,
			new UpdateUserRequest(true, UserRole.RESTO_MANAGER, restaurantId));

		assertThat(result.enabled()).isTrue();
		assertThat(result.role()).isEqualTo(UserRole.RESTO_MANAGER);
		assertThat(result.restaurantId()).isEqualTo(restaurantId);
	}

	@Test
	void updateUnknownUserIsNotFound() {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateUser(userId, new UpdateUserRequest(true, null, null)))
			.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void updateWithUnknownRestaurantIsNotFound() {
		UUID restaurantId = UUID.randomUUID();
		when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser()));
		when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateUser(userId, new UpdateUserRequest(null, null, restaurantId)))
			.isInstanceOf(ResponseStatusException.class);
	}
}
