package az.masapay.order.dto;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The live order (adisyon) for a table with its line items. */
public record OrderView(
		UUID orderId, String status, BigDecimal totalAmount, Instant openedAt, List<OrderItemView> items) {

	public static OrderView of(Order order, List<OrderItem> items) {
		List<OrderItemView> itemViews = items.stream()
			.map(i -> new OrderItemView(
				i.getId(), i.getName(), i.getQuantity(), i.getUnitPrice(), i.getTotalPrice(), i.isPaid()))
			.toList();
		return new OrderView(order.getId(), order.getStatus().name(), order.getTotalAmount(), order.getOpenedAt(), itemViews);
	}
}
