package az.masapay.table.dto;

import az.masapay.domain.RestaurantTable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A table as listed in the admin panel, with the customer link its QR code encodes.
 * {@code serviceChargePercent} is the table's own setting (null follows the restaurant, 0 is none)
 * and {@code effectiveServiceChargePercent} what its bills are charged (null when nothing) (#146).
 */
public record TableSummary(UUID id, String label, String status, UUID qrToken, String customerUrl,
		BigDecimal serviceChargePercent, BigDecimal effectiveServiceChargePercent) {

	public static TableSummary of(RestaurantTable table, String customerUrl) {
		return new TableSummary(table.getId(), table.getLabel(), table.getStatus().name(), table.getQrToken(),
			customerUrl, table.getServiceChargePercent(), table.effectiveServiceChargePercent());
	}
}
