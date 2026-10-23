package az.masapay.tip.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One tip a waiter received (#182): when, at which table, how much, and the bill part it came
 * with.
 */
public record MyTipLine(UUID transactionId, Instant paidAt, String tableLabel, BigDecimal tipAmount,
		BigDecimal billAmount) {
}
