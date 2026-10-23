package az.masapay.order;

import az.masapay.domain.MenuItem;
import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.KitchenStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Adds lines to a table's open bill, for staff (#21) and for customers ordering from the menu
 * (#140). Opens a bill when the table has none, keeps the total (and the service charge) in step
 * with the lines and pushes the fresh bill to every screen on the table.
 */
@Component
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenBillWriter {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	/**
	 * One line to add. {@code menuItem} is null for a line typed in by staff; {@code addedBy} is the
	 * staff member who added it, null when a customer ordered it (#144).
	 */
	public record NewLine(String name, BigDecimal quantity, BigDecimal unitPrice, MenuItem menuItem,
			User addedBy) {
	}

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster;

	public OpenBillWriter(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
			ObjectProvider<OrderRealtimeBroadcaster> broadcaster) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.broadcaster = broadcaster;
	}

	/** Must run inside the caller's transaction: the row lock is held until it commits. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void append(RestaurantTable table, List<NewLine> lines) {
		// The row lock serialises concurrent adds and payments on the same bill.
		Order order = orderRepository.findOpenOrderForUpdate(table.getId()).orElseGet(() -> openOrder(table));
		// The first waiter who adds to a bill nobody has taken serves the table and gets its tips (#182).
		if (order.getWaiter() == null) {
			lines.stream().map(NewLine::addedBy)
				.filter(user -> user != null && user.getRole() == UserRole.WAITER)
				.findFirst()
				.ifPresent(order::setWaiter);
		}

		for (NewLine line : lines) {
			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setName(line.name().trim());
			item.setQuantity(line.quantity());
			item.setUnitPrice(line.unitPrice());
			item.setTotalPrice(line.quantity().multiply(line.unitPrice()).setScale(2, RoundingMode.HALF_UP));
			item.setMenuItem(line.menuItem());
			// Dishes ordered from the menu go to the kitchen screen (#142).
			item.setKitchenStatus(line.menuItem() == null ? null : KitchenStatus.NEW);
			item.setAddedBy(line.addedBy());
			orderItemRepository.save(item);
		}

		order.setTotalAmount(newTotal(order, table.effectiveServiceChargePercent()));
		orderRepository.save(order);

		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(table.getQrToken()));
	}

	/** The waiter now serving the table's open bill (#182); 409 when the table has no open bill. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void assignWaiter(RestaurantTable table, User waiter) {
		Order order = orderRepository.findOpenOrderForUpdate(table.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "The table has no open bill"));
		order.setWaiter(waiter);
		orderRepository.save(order);
		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(table.getQrToken()));
	}

	/**
	 * Bill total after adding lines, always recomputed from all lines so the charge matches them:
	 * items + items * percent, or just the items when nothing is charged. A table whose service
	 * charge changed (#146) is therefore charged the new rate on the whole bill, in either direction.
	 */
	private BigDecimal newTotal(Order order, BigDecimal serviceChargePercent) {
		BigDecimal items = orderItemRepository.findByOrderId(order.getId()).stream()
			.map(OrderItem::getTotalPrice)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (serviceChargePercent == null) {
			return items;
		}
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
}
