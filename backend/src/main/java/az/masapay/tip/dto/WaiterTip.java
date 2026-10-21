package az.masapay.tip.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tips collected by one waiter in the report period. All waiter fields are null for
 * the "unassigned" row, i.e. paid orders that have no waiter.
 *
 * @param tipCount successful payments that carried a tip (tip &gt; 0)
 * @param tipTotal sum of those tips, in the restaurant currency
 */
public record WaiterTip(
	UUID waiterId,
	String waiterName,
	String waiterEmail,
	long tipCount,
	BigDecimal tipTotal) {
}
