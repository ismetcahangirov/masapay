package az.masapay.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.masapay.admin.dto.RestaurantSummary;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.SubscriptionPlan;
import az.masapay.domain.enums.SubscriptionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminRestaurantControllerTest {

	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AdminRestaurantController controller =
			new AdminRestaurantController(restaurantRepository, userRepository,
					Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC));

	private static Restaurant restaurant(String name) {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		restaurant.setName(name);
		return restaurant;
	}

	@Test
	void aSuperAdminSeesEveryRestaurantByName() {
		when(restaurantRepository.findAll()).thenReturn(List.of(restaurant("Dolma"), restaurant("Bulvar")));

		List<RestaurantSummary> list = controller.list(
				new AuthenticatedUser(UUID.randomUUID(), "a@x.az", UserRole.SUPER_ADMIN));

		assertThat(list).extracting(RestaurantSummary::name).containsExactly("Bulvar", "Dolma");
	}

	@Test
	void aManagerSeesOnlyTheirOwnRestaurant() {
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		User user = new User();
		user.setRestaurant(restaurant("Dolma"));
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));

		assertThat(controller.list(manager)).extracting(RestaurantSummary::name).containsExactly("Dolma");
	}

	@Test
	void theManagerSeesWhenTheSubscriptionEnds() {
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		Restaurant dolma = restaurant("Dolma");
		dolma.setSubscriptionPlan(SubscriptionPlan.BASIC);
		dolma.setSubscriptionStartsOn(LocalDate.of(2026, 9, 1));
		dolma.setSubscriptionEndsOn(LocalDate.of(2026, 10, 22));
		User user = new User();
		user.setRestaurant(dolma);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));

		assertThat(controller.list(manager)).singleElement().satisfies(summary -> {
			assertThat(summary.subscriptionStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
			assertThat(summary.subscriptionEndsOn()).isEqualTo(LocalDate.of(2026, 10, 22));
		});
	}
}
