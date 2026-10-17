package az.masapay.pos;

import java.math.BigDecimal;

/** The bill header as the external POS reports it. */
public record PosOrder(String posOrderId, BigDecimal totalAmount, boolean open) {}
