package az.masapay.order.dto;

import java.util.UUID;

/**
 * Response for a QR scan: the table context plus its current order. {@code order}
 * is null when the table has no open bill yet (a valid empty state, not an error).
 */
public record TableOrderResponse(
		UUID tableId, String tableLabel, UUID restaurantId, String restaurantName, String currency, OrderView order) {
}
