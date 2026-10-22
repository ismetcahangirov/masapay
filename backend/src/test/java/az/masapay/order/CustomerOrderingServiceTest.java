package az.masapay.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.MenuCategory;
import az.masapay.domain.MenuItem;
import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.order.dto.PlaceOrderRequest;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CustomerOrderingServiceTest {

	/** 01:30 on the 22nd in Baku, still the 21st in UTC. */
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
	private final CustomerOrderService customerOrderService = mock(CustomerOrderService.class);
	private final OrderRealtimeBroadcaster realtime = mock(OrderRealtimeBroadcaster.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster = mock(ObjectProvider.class);
	private final CustomerOrderingService service = new CustomerOrderingService(tableRepository, menuItemRepository,
			new OpenBillWriter(orderRepository, orderItemRepository, broadcaster), customerOrderService,
			Clock.fixed(Instant.parse("2026-10-21T21:30:00Z"), ZoneOffset.UTC));

	private final Restaurant restaurant = new Restaurant();
	private final RestaurantTable table = new RestaurantTable();
	private final Order order = new Order();

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		order.setId(UUID.randomUUID());
		order.setTotalAmount(BigDecimal.ZERO);
		when(tableRepository.findByQrToken(table.getQrToken())).thenReturn(Optional.of(table));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		doAnswer(inv -> {
			((Consumer<OrderRealtimeBroadcaster>) inv.getArgument(0)).accept(realtime);
			return null;
		}).when(broadcaster).ifAvailable(any());
	}

	private MenuItem dish(Restaurant owner, String name, String price) {
		MenuCategory category = new MenuCategory();
		category.setId(UUID.randomUUID());
		category.setRestaurant(owner);
		MenuItem item = new MenuItem();
		item.setId(UUID.randomUUID());
		item.setRestaurant(owner);
		item.setCategory(category);
		item.setName(name);
		item.setPrice(new BigDecimal(price));
		return item;
	}

	private void onMenu(MenuItem... items) {
		when(menuItemRepository.findAllById(any())).thenReturn(List.of(items));
	}

	private PlaceOrderRequest basket(Object... idAndQuantity) {
		List<PlaceOrderRequest.Line> lines = new ArrayList<>();
		for (int i = 0; i < idAndQuantity.length; i += 2) {
			lines.add(new PlaceOrderRequest.Line((UUID) idAndQuantity[i], (Integer) idAndQuantity[i + 1]));
		}
		return new PlaceOrderRequest(lines);
	}

	private List<OrderItem> savedLines(int count) {
		ArgumentCaptor<OrderItem> saved = ArgumentCaptor.forClass(OrderItem.class);
		verify(orderItemRepository, times(count)).save(saved.capture());
		return saved.getAllValues();
	}

	@Test
	void aConfirmedBasketGoesOntoTheBillAtMenuPricesAndIsPushedToTheTable() {
		MenuItem dolma = dish(restaurant, "Dolma", "9.00");
		MenuItem ayran = dish(restaurant, "Ayran", "1.50");
		onMenu(dolma, ayran);

		service.placeOrder(table.getQrToken(), basket(dolma.getId(), 2, ayran.getId(), 3));

		List<OrderItem> lines = savedLines(2);
		assertThat(lines).extracting(OrderItem::getName).containsExactly("Dolma", "Ayran");
		assertThat(lines.get(0).getMenuItem()).isSameAs(dolma);
		assertThat(lines.get(0).getTotalPrice()).isEqualByComparingTo("18.00");
		assertThat(lines.get(1).getTotalPrice()).isEqualByComparingTo("4.50");
		assertThat(order.getTotalAmount()).isEqualByComparingTo("22.50");
		verify(realtime).broadcastOrderUpdate(table.getQrToken());
		verify(customerOrderService).getCurrentOrder(table.getQrToken());
	}

	@Test
	void theSameDishTwiceBecomesOneLine() {
		MenuItem dolma = dish(restaurant, "Dolma", "9.00");
		onMenu(dolma);

		service.placeOrder(table.getQrToken(), basket(dolma.getId(), 1, dolma.getId(), 2));

		assertThat(savedLines(1).get(0).getQuantity()).isEqualByComparingTo("3");
	}

	@Test
	void aComboIsOrderedAsOneLineAtItsOwnPrice() {
		MenuItem soup = dish(restaurant, "Mərci", "3.00");
		MenuItem lunch = dish(restaurant, "Biznes lanç", "11.00");
		lunch.replaceComponents(Map.of(soup, 1));
		onMenu(lunch);

		service.placeOrder(table.getQrToken(), basket(lunch.getId(), 1));

		OrderItem line = savedLines(1).get(0);
		assertThat(line.getName()).isEqualTo("Biznes lanç");
		assertThat(line.getUnitPrice()).isEqualByComparingTo("11.00");
	}

	@Test
	void aDishSwitchedOffTodayIsRefusedAndNothingIsAdded() {
		MenuItem dolma = dish(restaurant, "Dolma", "9.00");
		MenuItem dovga = dish(restaurant, "Dovğa", "3.00");
		dovga.setOffOn(TODAY);
		onMenu(dolma, dovga);

		assertThatThrownBy(() -> service.placeOrder(table.getQrToken(),
				basket(dolma.getId(), 1, dovga.getId(), 1)))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(orderItemRepository, never()).save(any());
		verify(realtime, never()).broadcastOrderUpdate(any());
	}

	@Test
	void aDeletedDishOrOneOfAnotherRestaurantIsRefused() {
		Restaurant other = new Restaurant();
		other.setId(UUID.randomUUID());
		MenuItem foreign = dish(other, "Kabab", "1.00");
		onMenu(foreign);

		assertThatThrownBy(() -> service.placeOrder(table.getQrToken(), basket(foreign.getId(), 1)))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		assertThatThrownBy(() -> service.placeOrder(table.getQrToken(), basket(UUID.randomUUID(), 1)))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(orderItemRepository, never()).save(any());
	}

	@Test
	void anUnknownQrTokenIsNotFound() {
		assertThatThrownBy(() -> service.placeOrder(UUID.randomUUID(), basket(UUID.randomUUID(), 1)))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}
}
