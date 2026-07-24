package az.masapay.realtime;

import az.masapay.order.dto.TableOrderResponse;
import java.util.UUID;

/**
 * Envelope published to Redis when a table's order changes. Carries the table's
 * qrToken (the STOMP destination key) and the fresh order snapshot to forward.
 */
public record OrderUpdateMessage(UUID qrToken, TableOrderResponse order) {
}
