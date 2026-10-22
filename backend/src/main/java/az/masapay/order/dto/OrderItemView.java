package az.masapay.order.dto;

import az.masapay.domain.enums.KitchenStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line item as shown on the customer's live bill. {@code paid} lets the UI disable settled
 * items; {@code kitchenStatus} tells staff (and later the customer) whether it is ready (#142), and
 * is null for lines that never go to the kitchen.
 */
public record OrderItemView(
		UUID id, String name, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalPrice, boolean paid,
		KitchenStatus kitchenStatus) {
}
