package az.masapay.order;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.order.dto.AddOrderItemRequest;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Staff-side changes to a table's bill (#21). Adding an item opens the bill if the table has none,
 * updates the total and pushes the fresh bill to every customer screen on that table.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaffOrderService {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final RestaurantTableRepository tableRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final UserRepository userRepository;
	private final CustomerOrderService customerOrderService;
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster;

	public StaffOrderService(RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			OrderItemRepository orderItemRepository, UserRepository userRepository,
			CustomerOrderService customerOrderService, ObjectProvider<OrderRealtimeBroadcaster> broadcaster) {
		this.tableRepository = tableRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.userRepository = userRepository;
		this.customerOrderService = customerOrderService;
		this.broadcaster = broadcaster;
	}

	@Transactional
	public TableOrderResponse addItem(UUID tableId, AddOrderItemRequest request, AuthenticatedUser staff) {
		RestaurantTable table = tableRepository.findById(tableId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		checkSameRestaurant(staff, table);

		// The row lock serialises concurrent adds and payments on the same bill.
		Order order = orderRepository.findOpenOrderForUpdate(table.getId()).orElseGet(() -> openOrder(table));

		OrderItem item = new OrderItem();
		item.setOrder(order);
		item.setName(request.name().trim());
		item.setQuantity(request.quantity());
		item.setUnitPrice(request.unitPrice());
		item.setTotalPrice(request.quantity().multiply(request.unitPrice()).setScale(2, RoundingMode.HALF_UP));
		orderItemRepository.save(item);

		order.setTotalAmount(newTotal(order, item, table.getRestaurant().getServiceChargePercent()));
		orderRepository.save(order);

		UUID qrToken = table.getQrToken();
		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(qrToken));
		return customerOrderService.getCurrentOrder(qrToken);
	}

	/**
	 * Bill total after adding the item. Without a service charge it simply grows by the item. With
	 * one, it is recomputed from all items so the charge always matches them: items + items * percent.
	 */
	private BigDecimal newTotal(Order order, OrderItem added, BigDecimal serviceChargePercent) {
		if (serviceChargePercent == null) {
			return order.getTotalAmount().add(added.getTotalPrice());
		}
		BigDecimal items = orderItemRepository.findByOrderId(order.getId()).stream()
			.map(OrderItem::getTotalPrice)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal charge = items.multiply(serviceChargePercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
		return items.add(charge);
	}

	private Order openOrder(RestaurantTable table) {
		Order order = new Order();
		order.setRestaurant(table.getRestaurant());
		order.setTable(table);
		order.setCurrency(table.getRestaurant().getCurrency());
		return orderRepository.save(order);
	}

	/** Staff may only change bills of their own restaurant; a super admin any. */
	private void checkSameRestaurant(AuthenticatedUser staff, RestaurantTable table) {
		if (staff.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		User user = userRepository.findById(staff.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(table.getRestaurant().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
	}
}
