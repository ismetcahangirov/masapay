package az.masapay.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TableQrExportServiceTest {

	@Mock
	private RestaurantTableRepository tableRepository;
	@Mock
	private RestaurantRepository restaurantRepository;
	@Mock
	private UserRepository userRepository;

	private TableQrExportService service;

	private final Restaurant restaurant = restaurant();
	private final AuthenticatedUser superAdmin =
		new AuthenticatedUser(UUID.randomUUID(), "admin@masapay.az", UserRole.SUPER_ADMIN);
	private final AuthenticatedUser manager =
		new AuthenticatedUser(UUID.randomUUID(), "manager@masapay.az", UserRole.RESTO_MANAGER);

	@BeforeEach
	void setUp() {
		service = new TableQrExportService(tableRepository, restaurantRepository, userRepository,
			new QrCodeService(), new TableQrUrlBuilder("https://masapay.az"));
	}

	private static Restaurant restaurant() {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		restaurant.setName("Cafe");
		return restaurant;
	}

	private RestaurantTable table(String label) {
		RestaurantTable table = new RestaurantTable();
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		table.setLabel(label);
		return table;
	}

	private void managerBelongsTo(Restaurant assigned) {
		User user = new User();
		user.setId(manager.id());
		user.setRole(UserRole.RESTO_MANAGER);
		user.setRestaurant(assigned);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));
	}

	@Test
	void zipHoldsOneSvgPerTable() throws Exception {
		List<RestaurantTable> tables = List.of(table("A1"), table("A2"), table("Terrace 3"));
		when(restaurantRepository.existsById(restaurant.getId())).thenReturn(true);
		when(tableRepository.findByRestaurantIdOrderByLabelAsc(restaurant.getId())).thenReturn(tables);

		byte[] zip = service.restaurantZip(superAdmin, restaurant.getId());

		List<String> names = new ArrayList<>();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				names.add(entry.getName());
				assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).startsWith("<?xml").contains("<svg");
			}
		}
		assertThat(names).containsExactly("A1.svg", "A2.svg", "Terrace_3.svg");
	}

	@Test
	void managerExportsOwnRestaurant() {
		managerBelongsTo(restaurant);
		RestaurantTable table = table("B4");
		when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

		String svg = service.tableSvg(manager, table.getId());

		assertThat(svg).contains(">B4</text>");
	}

	@Test
	void managerOfAnotherRestaurantGets403ForTable() {
		managerBelongsTo(restaurant());
		RestaurantTable table = table("B4");
		when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

		assertThatThrownBy(() -> service.tableSvg(manager, table.getId()))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void managerOfAnotherRestaurantGets403ForZip() {
		managerBelongsTo(restaurant());

		assertThatThrownBy(() -> service.restaurantZip(manager, restaurant.getId()))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(tableRepository, never()).findByRestaurantIdOrderByLabelAsc(restaurant.getId());
	}

	@Test
	void unassignedManagerGets403() {
		managerBelongsTo(null);

		assertThatThrownBy(() -> service.restaurantZip(manager, restaurant.getId()))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void unknownTableIs404() {
		UUID tableId = UUID.randomUUID();
		when(tableRepository.findById(tableId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.tableSvg(superAdmin, tableId))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void fileNamesAreSanitizedAndDeduplicated() {
		Set<String> used = new HashSet<>();
		RestaurantTable first = table("Bar / 1");
		RestaurantTable duplicate = table("Bar / 1");
		RestaurantTable dots = table("../..");

		assertThat(TableQrExportService.uniqueName(first, used)).isEqualTo("Bar_1.svg");
		assertThat(TableQrExportService.uniqueName(duplicate, used))
			.isEqualTo("Bar_1-" + duplicate.getQrToken() + ".svg");
		assertThat(TableQrExportService.uniqueName(dots, used)).isEqualTo(dots.getQrToken() + ".svg");
	}
}
