package az.masapay.platform.dto;

import az.masapay.domain.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The whole platform over {@code [from, to]}, both Baku days included (#149): totals per
 * currency (amounts in different currencies are never added together) and one row per
 * restaurant for comparing them.
 */
public record PlatformAnalytics(LocalDate from, LocalDate to, String timeZone, List<Totals> totals,
		List<Row> restaurants) {

	/**
	 * @param subscriptionRevenue what paid subscriptions earn in the period: the monthly fee
	 *     counted per day, fee / 30 for every paid day inside it; trials earn nothing
	 */
	public record Totals(String currency, int restaurantCount, int activeRestaurantCount, long paymentCount,
			BigDecimal revenue, BigDecimal tips, BigDecimal subscriptionRevenue) {
	}

	/** @param averagePayment revenue / paymentCount; null when there was no payment */
	public record Row(UUID id, String name, String currency, boolean active, SubscriptionStatus subscriptionStatus,
			long paymentCount, BigDecimal revenue, BigDecimal tips, BigDecimal averagePayment,
			BigDecimal subscriptionRevenue) {
	}
}
