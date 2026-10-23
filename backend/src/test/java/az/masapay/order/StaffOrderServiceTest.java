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

import az.masapay.domain.MenuItem;
import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.User;
import az.masapay.domain.enums.KitchenStatus;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.order.dto.AddMenuItemRequest;
import az.masapay.order.dto.AddOrderItemRequest;
import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.order.dto.TablePaymentView;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
	private final MenuItemRepository menuItemRepository = mock(MenuItemRepository.class);
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	/** 01:30 on the 22nd in Baku, still the 21st in UTC. */
	private final StaffOrderService service = new StaffOrderService(tableRepository, userRepository,
			menuItemRepository, transactionRepository, customerOrderService,
			new OpenBillWriter(orderRepository, orderItemRepository, broadcaster),
			Clock.fixed(Instant.parse("2026-10-21T21:30:00Z"), ZoneOffset.UTC));
	private final User waiterUser = new User();

	private final Restaurant restaurant = new Restaurant();
	private final RestaurantTable table = new RestaurantTable();
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);
	private final AddOrderItemRequest kabab =
			new AddOrderItemRequest("Kabab", new BigDecimal("2"), new BigDecimal("14.50"));

	private final List<OrderItem> savedLines = new ArrayList<>();

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
		waiterUser.setName("Aysel");
		waiterUser.setRole(UserRole.WAITER);
		waiterUser.setRestaurant(restaurant);
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(waiterUser));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		// The bill total is recomputed from its lines: hand back what was saved.
		when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
			savedLines.add(inv.getArgument(0));
			return inv.getArgument(0);
		});
		when(orderItemRepository.findByOrderId(any())).thenAnswer(inv -> new ArrayList<>(savedLines));
		doAnswer(inv -> {
			((Consumer<OrderRealtimeBroadcaster>) inv.getArgument(0)).accept(realtime);
			return null;
		}).when(broadcaster).ifAvailable(any());
	}

	@Test
	void addsTheItemToTheOpenBillUpdatesTheTotalAndPushesIt() {
		Order order = new Order();
		order.setTotalAmount(new BigDecimal("6.50"));
		savedLines.add(line("6.50"));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));

		service.addItem(table.getId(), kabab, waiter);

		ArgumentCaptor<OrderItem> saved = ArgumentCaptor.forClass(OrderItem.class);
		verify(orderItemRepository).save(saved.capture());
		assertThat(saved.getValue().getTotalPrice()).isEqualByComparingTo("29.00");
		assertThat(saved.getValue().getOrder()).isSameAs(order);
		// A line typed in by staff does not go to the kitchen screen (#142).
		assertThat(saved.getValue().getKitchenStatus()).isNull();
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
	void aTableWithItsOwnServiceChargeIsChargedThatInstead() {
		restaurant.setServiceChargePercent(new BigDecimal("10.00"));
		table.setServiceChargePercent(new BigDecimal("15.00"));
		Order order = new Order();
		order.setId(UUID.randomUUID());
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));
		OrderItem added = new OrderItem();
		added.setTotalPrice(new BigDecimal("29.00"));
		when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(added));

		service.addItem(table.getId(), kabab, waiter);

		// 29.00 + 15% (4.35), not the restaurant's 10%.
		assertThat(order.getTotalAmount()).isEqualByComparingTo("33.35");
	}

	@Test
	void aTableSwitchedToZeroDropsTheChargeAlreadyOnTheBill() {
		restaurant.setServiceChargePercent(new BigDecimal("10.00"));
		table.setServiceChargePercent(BigDecimal.ZERO);
		// Earlier the table was at 15%: one 10.00 line plus a 1.50 charge.
		Order order = new Order();
		order.setTotalAmount(new BigDecimal("11.50"));
		savedLines.add(line("10.00"));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));

		service.addItem(table.getId(), kabab, waiter);

		// 10.00 + 29.00 and no charge at all: the old 1.50 is gone too.
		assertThat(order.getTotalAmount()).isEqualByComparingTo("39.00");
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

	// Who serves the table and gets its tips (#182)

	@Test
	void theFirstWaiterWhoAddsToABillTakesTheTable() {
		Order order = new Order();
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));

		service.addItem(table.getId(), kabab, waiter);

		assertThat(order.getWaiter()).isSameAs(waiterUser);
	}

	@Test
	void aManagerAddingOrASecondWaiterDoesNotTakeTheTable() {
		User aysel = new User();
		Order order = new Order();
		order.setWaiter(aysel);
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		User managerUser = new User();
		managerUser.setRole(UserRole.RESTO_MANAGER);
		managerUser.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(managerUser));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));

		service.addItem(table.getId(), kabab, waiter);
		service.addItem(table.getId(), kabab, manager);

		assertThat(order.getWaiter()).isSameAs(aysel);

		Order fresh = new Order();
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(fresh));
		service.addItem(table.getId(), kabab, manager);
		assertThat(fresh.getWaiter()).isNull();
	}

	@Test
	void aWaiterTakesOverATableAndEveryScreenHearsIt() {
		Order order = new Order();
		order.setWaiter(new User());
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));

		service.claim(table.getId(), waiter);

		assertThat(order.getWaiter()).isSameAs(waiterUser);
		verify(realtime).broadcastOrderUpdate(table.getQrToken());
	}

	@Test
	void onlyAWaiterTakesATableAndOnlyOneWithABill() {
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.claim(table.getId(), waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		User managerUser = new User();
		managerUser.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(managerUser));
		assertThatThrownBy(() -> service.claim(table.getId(), manager))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	// Manager / staff view of a table (#144)

	private OrderItem line(String total) {
		OrderItem item = new OrderItem();
		item.setTotalPrice(new BigDecimal(total));
		return item;
	}

	private MenuItem dish(Restaurant owner, String name, String price) {
		MenuItem item = new MenuItem();
		item.setId(UUID.randomUUID());
		item.setRestaurant(owner);
		item.setName(name);
		item.setPrice(new BigDecimal(price));
		when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
		return item;
	}

	@Test
	void aTypedLineRemembersWhoAddedIt() {
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(new Order()));

		service.addItem(table.getId(), kabab, waiter);

		ArgumentCaptor<OrderItem> saved = ArgumentCaptor.forClass(OrderItem.class);
		verify(orderItemRepository).save(saved.capture());
		assertThat(saved.getValue().getAddedBy()).isSameAs(waiterUser);
	}

	@Test
	void staffAddAMenuDishAtItsMenuPriceAndItGoesToTheKitchen() {
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(new Order()));
		MenuItem dolma = dish(restaurant, "Dolma", "9.00");

		service.addMenuItem(table.getId(), new AddMenuItemRequest(dolma.getId(), 2), waiter);

		ArgumentCaptor<OrderItem> saved = ArgumentCaptor.forClass(OrderItem.class);
		verify(orderItemRepository).save(saved.capture());
		OrderItem line = saved.getValue();
		assertThat(line.getName()).isEqualTo("Dolma");
		assertThat(line.getTotalPrice()).isEqualByComparingTo("18.00");
		assertThat(line.getMenuItem()).isSameAs(dolma);
		assertThat(line.getKitchenStatus()).isEqualTo(KitchenStatus.NEW);
		assertThat(line.getAddedBy()).isSameAs(waiterUser);
	}

	@Test
	void aMenuDishOffTodayOrOfAnotherRestaurantIsRefused() {
		MenuItem dovga = dish(restaurant, "Dovğa", "3.00");
		dovga.setOffOn(LocalDate.of(2026, 10, 22));
		Restaurant other = new Restaurant();
		other.setId(UUID.randomUUID());
		MenuItem foreign = dish(other, "Kabab", "1.00");

		assertThatThrownBy(() -> service.addMenuItem(table.getId(), new AddMenuItemRequest(dovga.getId(), 1), waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		assertThatThrownBy(() -> service.addMenuItem(table.getId(), new AddMenuItemRequest(foreign.getId(), 1),
				waiter))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(orderItemRepository, never()).save(any());
	}

	@Test
	void paymentsOfTheCurrentBillComeNewestFirst() {
		UUID orderId = UUID.randomUUID();
		OrderView open = new OrderView(orderId, "OPEN", new BigDecimal("20.00"), new BigDecimal("5.00"),
				new BigDecimal("15.00"), Instant.now(), List.of(), null, null, null);
		when(customerOrderService.getCurrentOrder(table.getQrToken())).thenReturn(
				new TableOrderResponse(table.getId(), "Masa 1", restaurant.getId(), "Cafe", "AZN", null, open));
		Transaction older = payment("2.00", Instant.parse("2026-10-22T10:00:00Z"));
		Transaction newer = payment("3.00", Instant.parse("2026-10-22T11:00:00Z"));
		when(transactionRepository.findByOrderId(orderId)).thenReturn(List.of(older, newer));

		List<TablePaymentView> payments = service.payments(table.getId(), waiter);

		assertThat(payments).extracting(TablePaymentView::id).containsExactly(newer.getId(), older.getId());
		assertThat(payments.get(0).status()).isEqualTo(TransactionStatus.SUCCESS);
	}

	@Test
	void aTableWithoutABillHasNoPayments() {
		when(customerOrderService.getCurrentOrder(table.getQrToken())).thenReturn(
				new TableOrderResponse(table.getId(), "Masa 1", restaurant.getId(), "Cafe", "AZN", null, null));

		assertThat(service.payments(table.getId(), waiter)).isEmpty();
		verify(transactionRepository, never()).findByOrderId(any());
	}

	private Transaction payment(String amount, Instant at) {
		Transaction transaction = new Transaction();
		transaction.setId(UUID.randomUUID());
		transaction.setAmount(new BigDecimal(amount));
		transaction.setTotalAmount(new BigDecimal(amount));
		transaction.setStatus(TransactionStatus.SUCCESS);
		transaction.setCreatedAt(at);
		return transaction;
	}
}
