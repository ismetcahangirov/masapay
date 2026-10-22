package az.masapay.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.KitchenStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.kitchen.dto.KitchenLineView;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class KitchenServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

	private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final OrderRealtimeBroadcaster realtime = mock(OrderRealtimeBroadcaster.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster = mock(ObjectProvider.class);
	private final KitchenService service = new KitchenService(orderItemRepository, userRepository, broadcaster,
			Clock.fixed(NOW, ZoneOffset.UTC));

	private final Restaurant restaurant = new Restaurant();
	private final Restaurant otherRestaurant = new Restaurant();
	private final RestaurantTable table = new RestaurantTable();
	private final AuthenticatedUser chef = new AuthenticatedUser(UUID.randomUUID(), "c@x.az", UserRole.CHEF);
	private final AuthenticatedUser otherChef = new AuthenticatedUser(UUID.randomUUID(), "o@x.az", UserRole.CHEF);

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		otherRestaurant.setId(UUID.randomUUID());
		table.setId(UUID.randomUUID());
		table.setLabel("Masa 4");
		table.setRestaurant(restaurant);
		User staff = new User();
		staff.setRestaurant(restaurant);
		User elsewhere = new User();
		elsewhere.setRestaurant(otherRestaurant);
		when(userRepository.findById(chef.id())).thenReturn(Optional.of(staff));
		when(userRepository.findById(otherChef.id())).thenReturn(Optional.of(elsewhere));
		doAnswer(inv -> {
			((Consumer<OrderRealtimeBroadcaster>) inv.getArgument(0)).accept(realtime);
			return null;
		}).when(broadcaster).ifAvailable(any());
	}

	private OrderItem line(String name, KitchenStatus status) {
		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setTable(table);
		OrderItem item = new OrderItem();
		item.setId(UUID.randomUUID());
		item.setOrder(order);
		item.setName(name);
		item.setQuantity(new BigDecimal("2"));
		item.setKitchenStatus(status);
		item.setCreatedAt(NOW.minusSeconds(300));
		when(orderItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
		return item;
	}

	@Test
	void theChefSeesOpenWorkAndWhatWasReadyInTheLastHalfHour() {
		OrderItem dolma = line("Dolma", KitchenStatus.NEW);
		when(orderItemRepository.findKitchenWork(restaurant.getId(),
				List.of(KitchenStatus.NEW, KitchenStatus.PREPARING), KitchenStatus.READY,
				NOW.minus(KitchenService.READY_SHOWN_FOR)))
			.thenReturn(List.of(dolma));

		List<KitchenLineView> work = service.work(restaurant.getId(), chef);

		assertThat(work).singleElement().satisfies(view -> {
			assertThat(view.tableLabel()).isEqualTo("Masa 4");
			assertThat(view.name()).isEqualTo("Dolma");
			assertThat(view.quantity()).isEqualByComparingTo("2");
			assertThat(view.status()).isEqualTo(KitchenStatus.NEW);
			assertThat(view.orderedAt()).isEqualTo(NOW.minusSeconds(300));
		});
	}

	@Test
	void markingADishPushesTheTableSoEveryScreenUpdates() {
		OrderItem dolma = line("Dolma", KitchenStatus.NEW);

		KitchenLineView view = service.mark(restaurant.getId(), dolma.getId(), KitchenStatus.PREPARING, chef);

		assertThat(view.status()).isEqualTo(KitchenStatus.PREPARING);
		assertThat(dolma.getKitchenStatus()).isEqualTo(KitchenStatus.PREPARING);
		verify(orderItemRepository).save(dolma);
		verify(realtime).broadcastOrderUpdate(table.getQrToken());
	}

	@Test
	void aLineThatNeverWentToTheKitchenCannotBeMarked() {
		OrderItem typedByStaff = line("Çörək", null);

		assertThatThrownBy(() -> service.mark(restaurant.getId(), typedByStaff.getId(), KitchenStatus.READY, chef))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(realtime, never()).broadcastOrderUpdate(any());
	}

	@Test
	void anotherRestaurantsKitchenIsClosed() {
		OrderItem dolma = line("Dolma", KitchenStatus.NEW);

		assertThatThrownBy(() -> service.work(restaurant.getId(), otherChef))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThatThrownBy(() -> service.mark(otherRestaurant.getId(), dolma.getId(), KitchenStatus.READY,
				otherChef))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		assertThat(dolma.getKitchenStatus()).isEqualTo(KitchenStatus.NEW);
	}
}
