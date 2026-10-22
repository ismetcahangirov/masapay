package az.masapay.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
import az.masapay.domain.enums.UserRole;
import az.masapay.order.dto.AddOrderItemRequest;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class StaffOrderServiceTest {

	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final CustomerOrderService customerOrderService = mock(CustomerOrderService.class);
	private final OrderRealtimeBroadcaster realtime = mock(OrderRealtimeBroadcaster.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster = mock(ObjectProvider.class);
	private final StaffOrderService service = new StaffOrderService(tableRepository, userRepository,
			customerOrderService, new OpenBillWriter(orderRepository, orderItemRepository, broadcaster));

	private final Restaurant restaurant = new Restaurant();
	private final RestaurantTable table = new RestaurantTable();
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);
	private final AddOrderItemRequest kabab =
			new AddOrderItemRequest("Kabab", new BigDecimal("2"), new BigDecimal("14.50"));

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
		User user = new User();
		user.setRestaurant(restaurant);
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(user));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		doAnswer(inv -> {
			((Consumer<OrderRealtimeBroadcaster>) inv.getArgument(0)).accept(realtime);
			return null;
		}).when(broadcaster).ifAvailable(any());
	}

	@Test
	void addsTheItemToTheOpenBillUpdatesTheTotalAndPushesIt() {
		Order order = new Order();
		order.setTotalAmount(new BigDecimal("6.50"));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));

		service.addItem(table.getId(), kabab, waiter);

		ArgumentCaptor<OrderItem> saved = ArgumentCaptor.forClass(OrderItem.class);
		verify(orderItemRepository).save(saved.capture());
		assertThat(saved.getValue().getTotalPrice()).isEqualByComparingTo("29.00");
		assertThat(saved.getValue().getOrder()).isSameAs(order);
		assertThat(order.getTotalAmount()).isEqualByComparingTo("35.50");
		verify(realtime).broadcastOrderUpdate(table.getQrToken());
	}

	@Test
	void withAServiceChargeTheTotalIsItemsPlusThePercent() {
		restaurant.setServiceChargePercent(new BigDecimal("10.00"));
		Order order = new Order();
		order.setId(UUID.randomUUID());
		order.setTotalAmount(new BigDecimal("22.00"));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));
		OrderItem existing = new OrderItem();
		existing.setTotalPrice(new BigDecimal("20.00"));
		OrderItem added = new OrderItem();
		added.setTotalPrice(new BigDecimal("29.00"));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(existing, added));

		service.addItem(table.getId(), kabab, waiter);

		assertThat(order.getTotalAmount()).isEqualByComparingTo("53.90");
	}

	@Test
	void opensABillWhenTheTableHasNone() {
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.empty());

		service.addItem(table.getId(), kabab, waiter);

		ArgumentCaptor<Order> opened = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository, atLeastOnce()).save(opened.capture());
		assertThat(opened.getValue().getTable()).isSameAs(table);
		assertThat(opened.getValue().getTotalAmount()).isEqualByComparingTo("29.00");
	}

	@Test
	void staffOfAnotherRestaurantGets403() {
		AuthenticatedUser stranger = new AuthenticatedUser(UUID.randomUUID(), "s@x.az", UserRole.WAITER);
		User user = new User();
		Restaurant other = new Restaurant();
		other.setId(UUID.randomUUID());
		user.setRestaurant(other);
		when(userRepository.findById(stranger.id())).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.addItem(table.getId(), kabab, stranger))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(orderItemRepository, never()).save(any());
	}
}
