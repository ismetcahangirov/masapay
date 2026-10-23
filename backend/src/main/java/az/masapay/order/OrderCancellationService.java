package az.masapay.order;

import az.masapay.domain.Order;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cancels a table's confirmed order (#186). Customers can not: once they confirmed, they ask the
 * manager. Waiters and chefs can not either. Only a RESTO_MANAGER of the restaurant, or the super
 * admin, with a reason; who and when are kept. A bill with money on it (paid, or a payment under
 * way) is not cancelled: that needs a refund first. The kitchen drops the dishes and every screen
 * on the table hears it at once.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderCancellationService {

	/** Reason sent with the 409, so the screen can say a refund is needed first. */
	public static final String HAS_PAYMENTS = "ORDER_HAS_PAYMENTS";

	private static final Set<TransactionStatus> MONEY_MOVED = EnumSet.of(TransactionStatus.SUCCESS,
		TransactionStatus.PENDING);

	private final RestaurantTableRepository tableRepository;
	private final OrderRepository orderRepository;
	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final CustomerOrderService customerOrderService;
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster;
	private final Clock clock;

	@Autowired
	public OrderCancellationService(RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			TransactionRepository transactionRepository, UserRepository userRepository,
			CustomerOrderService customerOrderService, ObjectProvider<OrderRealtimeBroadcaster> broadcaster) {
		this(tableRepository, orderRepository, transactionRepository, userRepository, customerOrderService,
			broadcaster, Clock.systemUTC());
	}

	OrderCancellationService(RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			TransactionRepository transactionRepository, UserRepository userRepository,
			CustomerOrderService customerOrderService, ObjectProvider<OrderRealtimeBroadcaster> broadcaster,
			Clock clock) {
		this.tableRepository = tableRepository;
		this.orderRepository = orderRepository;
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.customerOrderService = customerOrderService;
		this.broadcaster = broadcaster;
		this.clock = clock;
	}

	@Transactional
	public TableOrderResponse cancel(UUID tableId, String reason, AuthenticatedUser caller) {
		if (caller.role() != UserRole.RESTO_MANAGER && caller.role() != UserRole.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a manager cancels an order");
		}
		if (reason == null || reason.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required");
		}
		RestaurantTable table = tableRepository.findById(tableId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		User manager = userRepository.findById(caller.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		if (caller.role() != UserRole.SUPER_ADMIN && (manager.getRestaurant() == null
				|| !manager.getRestaurant().getId().equals(table.getRestaurant().getId()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}

		// The row lock keeps a payment from starting while the order is being cancelled.
		Order order = orderRepository.findOpenOrderForUpdate(table.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "The table has no open order"));
		boolean moneyMoved = transactionRepository.findByOrderId(order.getId()).stream()
			.anyMatch(transaction -> MONEY_MOVED.contains(transaction.getStatus()));
		if (moneyMoved) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, HAS_PAYMENTS);
		}

		Instant now = clock.instant();
		order.setStatus(OrderStatus.CANCELLED);
		order.setClosedAt(now);
		order.setCancelledAt(now);
		order.setCancelledBy(manager);
		order.setCancelReason(reason.trim());
		orderRepository.save(order);

		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(table.getQrToken()));
		return customerOrderService.getCurrentOrder(table.getQrToken());
	}
}
