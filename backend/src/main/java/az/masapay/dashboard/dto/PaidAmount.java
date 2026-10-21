package az.masapay.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One successful payment, reduced to what the daily chart needs. */
public record PaidAmount(Instant paidAt, BigDecimal amount, BigDecimal tipAmount) {
}
