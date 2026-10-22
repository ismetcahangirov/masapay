package az.masapay.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response for a QR scan: the table context plus its current order. {@code order}
 * is null when the table has no open bill yet (a valid empty state, not an error).
 * {@code serviceChargePercent} is the restaurant's service charge (null when it charges none),
 * so the receipt can label the charge line, e.g. "Xidmət haqqı (10%)".
 */
public record TableOrderResponse(
		UUID tableId, String tableLabel, UUID restaurantId, String restaurantName, String currency,
		BigDecimal serviceChargePercent, OrderView order) {

	/**
	 * What goes out on customer channels (the public endpoints and the table's live topic): the
	 * same bill, but without the names of the staff who added lines (#144).
	 */
	public TableOrderResponse forCustomers() {
		return order == null ? this : new TableOrderResponse(tableId, tableLabel, restaurantId, restaurantName,
			currency, serviceChargePercent, order.withoutStaffNames());
	}
}
