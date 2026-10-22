package az.masapay.order;

import az.masapay.domain.MenuItem;
import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.KitchenStatus;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

		BigDecimal added = BigDecimal.ZERO;
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
			added = added.add(item.getTotalPrice());
		}

		order.setTotalAmount(newTotal(order, added, table.getRestaurant().getServiceChargePercent()));
		orderRepository.save(order);

		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(table.getQrToken()));
	}

	/**
	 * Bill total after adding lines worth {@code added}. Without a service charge it simply grows.
	 * With one, it is recomputed from all lines so the charge always matches them:
	 * items + items * percent.
	 */
	private BigDecimal newTotal(Order order, BigDecimal added, BigDecimal serviceChargePercent) {
		if (serviceChargePercent == null) {
			return order.getTotalAmount().add(added);
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
}
