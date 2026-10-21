package az.masapay.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Today's KPIs of a restaurant plus a daily revenue series (#43). "Today" is
 * {@code date} in {@code timeZone}.
 *
 * @param revenueToday successful payments today, excluding tips
 * @param tipsToday tips paid today
 * @param averageTableMinutes mean closed_at - opened_at of orders paid today; null when none
 * @param payriffSuccessRate SUCCESS / (SUCCESS + FAILED) of today's Payriff payments, 0..1;
 *     null when there were none
 * @param series one entry per day, oldest first, ending with today
 */
public record DashboardMetrics(
	UUID restaurantId,
	String currency,
	LocalDate date,
	String timeZone,
	BigDecimal revenueToday,
	BigDecimal tipsToday,
	Double averageTableMinutes,
	Double payriffSuccessRate,
	long payriffSuccessCount,
	long payriffFailedCount,
	List<DailyRevenue> series) {
}
