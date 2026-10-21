package az.masapay.tip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tip report of a restaurant for the inclusive date range {@code from}..{@code to},
 * interpreted in {@code timeZone}.
 */
public record TipReport(
	UUID restaurantId,
	String currency,
	LocalDate from,
	LocalDate to,
	String timeZone,
	List<WaiterTip> waiters,
	long totalTipCount,
	BigDecimal totalTipAmount) {
}
