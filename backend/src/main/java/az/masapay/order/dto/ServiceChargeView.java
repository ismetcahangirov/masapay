package az.masapay.order.dto;

import java.math.BigDecimal;

/**
 * The part of the bill not tied to a line item (e.g. a service charge), shared
 * equally between the guests. {@code guests} is null until the first item payer
 * fixes the guest count.
 */
public record ServiceChargeView(BigDecimal amount, Integer guests, int paidShares) {
}
