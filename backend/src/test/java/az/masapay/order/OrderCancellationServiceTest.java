package az.masapay.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.User;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
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

class OrderCancellationServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final CustomerOrderService customerOrderService = mock(CustomerOrderService.class);
	private final OrderRealtimeBroadcaster realtime = mock(OrderRealtimeBroadcaster.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster = mock(ObjectProvider.class);
	private final OrderCancellationService service = new OrderCancellationService(tableRepository,
			orderRepository, transactionRepository, userRepository, customerOrderService, broadcaster,
			Clock.fixed(NOW, ZoneOffset.UTC));

	private final Restaurant restaurant = new Restaurant();
	private final RestaurantTable table = new RestaurantTable();
	private final Order order = new Order();
	private final AuthenticatedUser manager =
			new AuthenticatedUser(UUID.randomUUID(), "m@dolma.az", UserRole.RESTO_MANAGER);
	private final User managerUser = new User();

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		table.setId(UUID.randomUUID());
		table.setQrToken(UUID.randomUUID());
		table.setRestaurant(restaurant);
		order.setId(UUID.randomUUID());
		managerUser.setRestaurant(restaurant);
		when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(managerUser));
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.of(order));
		doAnswer(inv -> {
			((Consumer<OrderRealtimeBroadcaster>) inv.getArgument(0)).accept(realtime);
			return null;
		}).when(broadcaster).ifAvailable(any());
	}

	private static Transaction payment(TransactionStatus status) {
		Transaction transaction = new Transaction();
		transaction.setStatus(status);
		return transaction;
	}

	private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus status) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(status));
	}

	@Test
	void aManagerCancelsWithAReasonAndEveryScreenHearsIt() {
		when(transactionRepository.findByOrderId(order.getId()))
				.thenReturn(List.of(payment(TransactionStatus.FAILED)));

		service.cancel(table.getId(), "  Müştəri getdi  ", manager);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(order.getCancelledBy()).isSameAs(managerUser);
		assertThat(order.getCancelledAt()).isEqualTo(NOW);
		assertThat(order.getClosedAt()).isEqualTo(NOW);
		assertThat(order.getCancelReason()).isEqualTo("Müştəri getdi");
		verify(realtime).broadcastOrderUpdate(table.getQrToken());
	}

	@Test
	void aBillWithMoneyOnItIsNotCancelled() {
		for (TransactionStatus moved : List.of(TransactionStatus.SUCCESS, TransactionStatus.PENDING)) {
			when(transactionRepository.findByOrderId(order.getId())).thenReturn(List.of(payment(moved)));

			assertThatThrownBy(() -> service.cancel(table.getId(), "x", manager))
					.isInstanceOfSatisfying(ResponseStatusException.class, e -> {
						assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
						assertThat(e.getReason()).isEqualTo(OrderCancellationService.HAS_PAYMENTS);
					});
		}
		assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void waitersChefsAndOtherRestaurantsCanNot() {
		for (UserRole role : List.of(UserRole.WAITER, UserRole.CHEF)) {
			assertStatus(() -> service.cancel(table.getId(), "x",
					new AuthenticatedUser(UUID.randomUUID(), "s@x.az", role)), HttpStatus.FORBIDDEN);
		}

		AuthenticatedUser stranger = new AuthenticatedUser(UUID.randomUUID(), "o@x.az", UserRole.RESTO_MANAGER);
		User other = new User();
		Restaurant elsewhere = new Restaurant();
		elsewhere.setId(UUID.randomUUID());
		other.setRestaurant(elsewhere);
		when(userRepository.findById(stranger.id())).thenReturn(Optional.of(other));
		assertStatus(() -> service.cancel(table.getId(), "x", stranger), HttpStatus.FORBIDDEN);

		assertStatus(() -> service.cancel(table.getId(), " ", manager), HttpStatus.BAD_REQUEST);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void aTableWithoutAnOpenOrderHasNothingToCancel() {
		when(orderRepository.findOpenOrderForUpdate(table.getId())).thenReturn(Optional.empty());

		assertStatus(() -> service.cancel(table.getId(), "x", manager), HttpStatus.CONFLICT);
	}
}
