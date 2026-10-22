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
import az.masapay.order.CustomerOrderService;
import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.table.dto.FloorTableView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class FloorServiceTest {

	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final CustomerOrderService customerOrderService = mock(CustomerOrderService.class);
	private final FloorService service = new FloorService(tableRepository, userRepository, customerOrderService);

	private final Restaurant restaurant = new Restaurant();
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);
	private final AuthenticatedUser superAdmin =
			new AuthenticatedUser(UUID.randomUUID(), "s@x.az", UserRole.SUPER_ADMIN);

	@BeforeEach
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		User staff = new User();
		staff.setRestaurant(restaurant);
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(staff));
	}

	private RestaurantTable table(String label) {
		RestaurantTable table = new RestaurantTable();
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		table.setLabel(label);
		return table;
	}

	private TableOrderResponse snapshot(RestaurantTable table, OrderView order) {
		return new TableOrderResponse(table.getId(), table.getLabel(), restaurant.getId(), "Cafe", "AZN", null,
			order);
	}

	@Test
	void aWaiterSeesEveryTableWithItsFloorStatus() {
		RestaurantTable empty = table("Masa 1");
		RestaurantTable busy = table("Masa 2");
		when(tableRepository.findByRestaurantIdOrderByLabelAsc(restaurant.getId())).thenReturn(List.of(empty, busy));
		when(customerOrderService.getCurrentOrder(empty.getQrToken())).thenReturn(snapshot(empty, null));
		OrderView open = new OrderView(UUID.randomUUID(), "OPEN", new BigDecimal("20.00"), new BigDecimal("5.00"),
			new BigDecimal("15.00"), Instant.now(), List.of(), null, null);
		when(customerOrderService.getCurrentOrder(busy.getQrToken())).thenReturn(snapshot(busy, open));

		List<FloorTableView> floor = service.floor(restaurant.getId(), waiter);

		assertThat(floor).extracting(FloorTableView::label).containsExactly("Masa 1", "Masa 2");
		assertThat(floor).extracting(FloorTableView::status)
				.containsExactly(FloorStatus.EMPTY, FloorStatus.PARTIALLY_PAID);
	}

	@Test
	void aSuperAdminSeesAnyRestaurant() {
		UUID other = UUID.randomUUID();
		when(tableRepository.findByRestaurantIdOrderByLabelAsc(other)).thenReturn(List.of());

		assertThat(service.floor(other, superAdmin)).isEmpty();
	}

	@Test
	void staffOfAnotherRestaurantGets403() {
		assertThatThrownBy(() -> service.floor(UUID.randomUUID(), waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(customerOrderService, never()).getCurrentOrder(any());
	}

	@Test
	void theOrdersScreenGetsTheFullBillOfEveryTableThatHasOne() {
		RestaurantTable empty = table("Masa 1");
		RestaurantTable busy = table("Masa 2");
		when(tableRepository.findByRestaurantIdOrderByLabelAsc(restaurant.getId())).thenReturn(List.of(empty, busy));
		when(customerOrderService.getCurrentOrder(empty.getQrToken())).thenReturn(snapshot(empty, null));
		OrderView open = new OrderView(UUID.randomUUID(), "OPEN", new BigDecimal("20.00"), new BigDecimal("5.00"),
			new BigDecimal("15.00"), Instant.now(), List.of(), null, null);
		TableOrderResponse busyBill = snapshot(busy, open);
		when(customerOrderService.getCurrentOrder(busy.getQrToken())).thenReturn(busyBill);

		List<TableOrderResponse> bills = service.bills(restaurant.getId(), waiter);

		assertThat(bills).containsExactly(busyBill);
	}

	@Test
	void staffOfAnotherRestaurantCannotSeeItsBills() {
		assertThatThrownBy(() -> service.bills(UUID.randomUUID(), waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(customerOrderService, never()).getCurrentOrder(any());
	}
}
