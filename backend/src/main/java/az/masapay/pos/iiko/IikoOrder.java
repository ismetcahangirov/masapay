package az.masapay.pos.iiko;

import java.math.BigDecimal;
import java.util.List;

/** An iiko order: status ("New", "Bill", "Closed", ...), total and lines. */
public record IikoOrder(String status, BigDecimal sum, List<IikoOrderItem> items) {}
