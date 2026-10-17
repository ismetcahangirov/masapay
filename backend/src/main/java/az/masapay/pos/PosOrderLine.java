package az.masapay.pos;

import java.math.BigDecimal;

/** One line of a bill as the external POS reports it. Ids are the POS's own, not our UUIDs. */
public record PosOrderLine(String posItemId, String name, BigDecimal quantity, BigDecimal unitPrice) {}
