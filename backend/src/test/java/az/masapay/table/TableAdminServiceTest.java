package az.masapay.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.qr.TableQrUrlBuilder;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.table.dto.TableSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TableAdminServiceTest {

	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final TableAdminService service = new TableAdminService(tableRepository, restaurantRepository,
			userRepository, new TableQrUrlBuilder("http://localhost:5173"));

	private final Restaurant restaurant = new Restaurant();
	private final AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);

	@BeforeEach
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		User staff = new User();
		staff.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(staff));
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(staff));
		when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
		when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> {
			RestaurantTable table = inv.getArgument(0);
			table.setId(UUID.randomUUID());
			return table;
		});
	}

	private RestaurantTable table(String label) {
		RestaurantTable table = new RestaurantTable();
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		table.setLabel(label);
		return table;
	}

	@Test
	void aWaiterSeesTheirRestaurantsTablesWithTheQrLink() {
		RestaurantTable masa1 = table("Masa 1");
		when(tableRepository.findByRestaurantIdOrderByLabelAsc(restaurant.getId())).thenReturn(List.of(masa1));

		List<TableSummary> tables = service.list(restaurant.getId(), waiter);

		assertThat(tables).singleElement().satisfies(t -> {
			assertThat(t.label()).isEqualTo("Masa 1");
			assertThat(t.customerUrl()).contains("token=" + masa1.getQrToken());
		});
	}

	@Test
	void aManagerCreatesATableWithItsOwnQrToken() {
		when(tableRepository.findByRestaurantId(restaurant.getId())).thenReturn(List.of(table("Masa 1")));

		TableSummary created = service.create(restaurant.getId(), "  Masa 7 ", manager);

		assertThat(created.label()).isEqualTo("Masa 7");
		assertThat(created.qrToken()).isNotNull();
	}

	@Test
	void aDuplicateNameIs409() {
		when(tableRepository.findByRestaurantId(restaurant.getId())).thenReturn(List.of(table("Masa 1")));

		assertThatThrownBy(() -> service.create(restaurant.getId(), "masa 1", manager))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void aWaiterCannotCreateTables() {
		assertThatThrownBy(() -> service.create(restaurant.getId(), "Masa 9", waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(tableRepository, never()).save(any());
	}

	@Test
	void staffOfAnotherRestaurantGets403() {
		assertThatThrownBy(() -> service.list(UUID.randomUUID(), waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}
}
