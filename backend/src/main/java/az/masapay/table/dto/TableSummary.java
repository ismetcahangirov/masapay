package az.masapay.table.dto;

import az.masapay.domain.RestaurantTable;
import java.util.UUID;

/** A table as listed in the admin panel, with the customer link its QR code encodes. */
public record TableSummary(UUID id, String label, String status, UUID qrToken, String customerUrl) {

	public static TableSummary of(RestaurantTable table, String customerUrl) {
		return new TableSummary(table.getId(), table.getLabel(), table.getStatus().name(), table.getQrToken(),
			customerUrl);
	}
}
