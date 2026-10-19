package az.masapay.pos.iiko;

import java.math.BigDecimal;

/** One line of an iiko order. {@code amount} is the quantity, not money. */
public record IikoOrderItem(IikoProduct product, BigDecimal amount, BigDecimal price) {}
