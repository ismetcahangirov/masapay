package az.masapay.order.dto;

import az.masapay.domain.OrderItem;
import az.masapay.domain.enums.KitchenStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single line item as shown on the live bill. {@code paid} lets the UI disable settled items;
 * {@code kitchenStatus} tells staff whether it is ready (#142), null for lines that never go to
 * the kitchen. {@code source}, {@code addedBy} and {@code addedAt} say who put it on the bill and
 * when (#144): a staff member (by name), a customer from the menu, or a POS sync.
 */
public record OrderItemView(
		UUID id, String name, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalPrice, boolean paid,
		KitchenStatus kitchenStatus, Source source, String addedBy, Instant addedAt) {

	public enum Source {
		STAFF, CUSTOMER, POS
	}

	public static OrderItemView of(OrderItem item) {
		Source source = item.getAddedBy() != null ? Source.STAFF
			: item.getPosItemId() != null ? Source.POS
			// Only customers order menu dishes without a staff member; older typed lines have no name.
			: item.getMenuItem() != null ? Source.CUSTOMER
			: Source.STAFF;
		String addedBy = item.getAddedBy() == null ? null : item.getAddedBy().getName();
		return new OrderItemView(item.getId(), item.getName(), item.getQuantity(), item.getUnitPrice(),
			item.getTotalPrice(), item.isPaid(), item.getKitchenStatus(), source, addedBy, item.getCreatedAt());
	}

	/** Customers see that staff added a line, not who: names stay with the restaurant. */
	public OrderItemView withoutStaffName() {
		return new OrderItemView(id, name, quantity, unitPrice, totalPrice, paid, kitchenStatus, source, null,
			addedAt);
	}
}
