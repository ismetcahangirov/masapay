package az.masapay.order.dto;

import az.masapay.domain.Order;
import az.masapay.domain.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The live order (adisyon) for a table with its line items. {@code equalSplit} is
 * null unless an equal split has been started; {@code serviceCharge} is null
 * unless the total is more than the items (e.g. a service charge).
 */
public record OrderView(
		UUID orderId, String status, BigDecimal totalAmount, Instant openedAt, List<OrderItemView> items,
		EqualSplitView equalSplit, ServiceChargeView serviceCharge) {

	public static OrderView of(Order order, List<OrderItem> items) {
		List<OrderItemView> itemViews = items.stream()
			.map(i -> new OrderItemView(
				i.getId(), i.getName(), i.getQuantity(), i.getUnitPrice(), i.getTotalPrice(), i.isPaid()))
			.toList();
		EqualSplitView equalSplit = order.getEqualSplitParts() == null
			? null
			: new EqualSplitView(order.getEqualSplitParts(), order.getEqualSplitPaidParts());
		BigDecimal extra = order.getTotalAmount()
			.subtract(items.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
		ServiceChargeView serviceCharge = extra.signum() <= 0
			? null
			: new ServiceChargeView(extra, order.getServiceChargeGuests(), order.getServiceChargePaidShares());
		return new OrderView(
			order.getId(), order.getStatus().name(), order.getTotalAmount(), order.getOpenedAt(), itemViews,
			equalSplit, serviceCharge);
	}
}
