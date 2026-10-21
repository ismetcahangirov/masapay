package az.masapay.dashboard.dto;

import java.math.BigDecimal;

/**
 * Sums of successful payments in a period. Both are null when the period has no
 * payment (SQL {@code sum} over no rows).
 *
 * @param revenue bill parts paid, excluding tips
 * @param tips tips paid on top
 */
public record PaymentTotals(BigDecimal revenue, BigDecimal tips) {
}
