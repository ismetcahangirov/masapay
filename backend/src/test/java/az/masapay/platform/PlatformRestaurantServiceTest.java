package az.masapay.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.PosType;
import az.masapay.domain.enums.UserRole;
import az.masapay.platform.dto.CreateRestaurantRequest;
import az.masapay.platform.dto.PlatformRestaurantView;
import az.masapay.platform.dto.UpdateRestaurantRequest;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PlatformRestaurantServiceTest {

	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final PlatformRestaurantService service = new PlatformRestaurantService(restaurantRepository,
			userRepository);

	private final AuthenticatedUser superAdmin =
			new AuthenticatedUser(UUID.randomUUID(), "admin@masapay.az", UserRole.SUPER_ADMIN);
	private final AuthenticatedUser manager =
			new AuthenticatedUser(UUID.randomUUID(), "m@dolma.az", UserRole.RESTO_MANAGER);
	private final List<User> savedUsers = new ArrayList<>();

	@BeforeEach
	void setUp() {
		when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(inv -> {
			Restaurant restaurant = inv.getArgument(0);
			if (restaurant.getId() == null) {
				restaurant.setId(UUID.randomUUID());
			}
			return restaurant;
		});
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			savedUsers.add(inv.getArgument(0));
			return inv.getArgument(0);
		});
		when(userRepository.findByRestaurantIdAndRoleInOrderByNameAscEmailAsc(any(), any()))
				.thenAnswer(inv -> savedUsers.stream()
					.filter(u -> u.getRestaurant() != null && u.getRestaurant().getId().equals(inv.getArgument(0)))
					.toList());
	}

	private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus status) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(status));
	}

	@Test
	void aNewRestaurantComesWithItsFirstManagerReadyToSignIn() {
		when(userRepository.findByEmailIgnoreCase("leyla@gmail.com")).thenReturn(Optional.empty());

		PlatformRestaurantView created = service.create(
				new CreateRestaurantRequest("  Nərgiz Kafe ", "AZN", PosType.IIKO, "Leyla@Gmail.com "), superAdmin);

		assertThat(created.name()).isEqualTo("Nərgiz Kafe");
		assertThat(created.posType()).isEqualTo(PosType.IIKO);
		assertThat(created.active()).isTrue();
		assertThat(created.managers()).singleElement().satisfies(m -> {
			assertThat(m.email()).isEqualTo("leyla@gmail.com");
			assertThat(m.signedIn()).isFalse();
		});
		User leyla = savedUsers.get(0);
		assertThat(leyla.getRole()).isEqualTo(UserRole.RESTO_MANAGER);
		assertThat(leyla.isEnabled()).isTrue();
		assertThat(leyla.getRestaurant().getId()).isEqualTo(created.id());
	}

	@Test
	void anEmailThatAlreadyWorksSomewhereIsRefusedAndNoRestaurantIsMade() {
		User busy = new User();
		busy.setEmail("busy@gmail.com");
		busy.setRole(UserRole.WAITER);
		busy.setRestaurant(new Restaurant());
		when(userRepository.findByEmailIgnoreCase("busy@gmail.com")).thenReturn(Optional.of(busy));

		assertStatus(() -> service.create(new CreateRestaurantRequest("X", "AZN", PosType.CUSTOM, "busy@gmail.com"),
				superAdmin), HttpStatus.CONFLICT);
		verify(restaurantRepository, never()).save(any());
	}

	@Test
	void someoneWhoSignedInButBelongsNowhereBecomesTheManager() {
		User newcomer = new User();
		newcomer.setEmail("newcomer@gmail.com");
		newcomer.setGoogleSub("sub-1");
		when(userRepository.findByEmailIgnoreCase("newcomer@gmail.com")).thenReturn(Optional.of(newcomer));

		PlatformRestaurantView created = service.create(
				new CreateRestaurantRequest("Y", "AZN", PosType.CUSTOM, "newcomer@gmail.com"), superAdmin);

		assertThat(created.managers()).singleElement().satisfies(m -> assertThat(m.signedIn()).isTrue());
		assertThat(newcomer.isEnabled()).isTrue();
	}

	@Test
	void aRestaurantIsRenamedAndSwitchedOff() {
		Restaurant dolma = new Restaurant();
		dolma.setId(UUID.randomUUID());
		dolma.setName("Dolma");
		when(restaurantRepository.findById(dolma.getId())).thenReturn(Optional.of(dolma));

		PlatformRestaurantView off = service.update(dolma.getId(), new UpdateRestaurantRequest("Dolma Restoran", false),
				superAdmin);

		assertThat(off.name()).isEqualTo("Dolma Restoran");
		assertThat(off.active()).isFalse();
		assertStatus(() -> service.update(dolma.getId(), new UpdateRestaurantRequest("  ", null), superAdmin),
				HttpStatus.BAD_REQUEST);
	}

	@Test
	void aSecondManagerCanBeAdded() {
		Restaurant dolma = new Restaurant();
		dolma.setId(UUID.randomUUID());
		when(restaurantRepository.findById(dolma.getId())).thenReturn(Optional.of(dolma));
		when(userRepository.findByEmailIgnoreCase(eq("second@gmail.com"))).thenReturn(Optional.empty());

		PlatformRestaurantView view = service.addManager(dolma.getId(), "second@gmail.com", superAdmin);

		assertThat(view.managers()).extracting(PlatformRestaurantView.Manager::email)
				.containsExactly("second@gmail.com");
	}

	@Test
	void onlyTheSuperAdmin() {
		assertStatus(() -> service.list(manager), HttpStatus.FORBIDDEN);
		assertStatus(() -> service.create(new CreateRestaurantRequest("X", "AZN", PosType.CUSTOM, "x@x.az"), manager),
				HttpStatus.FORBIDDEN);
		ArgumentCaptor<Restaurant> any = ArgumentCaptor.forClass(Restaurant.class);
		verify(restaurantRepository, never()).save(any.capture());
	}
}
