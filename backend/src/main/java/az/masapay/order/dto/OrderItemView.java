package az.masapay.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** A single line item as shown on the customer's live bill. */
public record OrderItemView(UUID id, String name, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
}
