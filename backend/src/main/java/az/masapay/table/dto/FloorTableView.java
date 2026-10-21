package az.masapay.table.dto;

import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.table.FloorStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One table on the live floor plan (#44). Amounts are null for an empty table; {@code minutesOpen}
 * is set only while the bill is still open.
 */
public record FloorTableView(
		UUID tableId, String label, FloorStatus status, String currency, BigDecimal totalAmount,
		BigDecimal paidAmount, BigDecimal outstanding, Instant openedAt, Long minutesOpen) {

	/** Builds the floor entry from the same snapshot the table's customers see. */
	public static FloorTableView of(TableOrderResponse snapshot, Instant now) {
		OrderView order = snapshot.order();
		if (order == null) {
			return new FloorTableView(snapshot.tableId(), snapshot.tableLabel(), FloorStatus.EMPTY,
				snapshot.currency(), null, null, null, null, null);
		}
		FloorStatus status = statusOf(order);
		Long minutesOpen = status == FloorStatus.CLOSED || order.openedAt() == null
			? null
			: Math.max(0, Duration.between(order.openedAt(), now).toMinutes());
		return new FloorTableView(snapshot.tableId(), snapshot.tableLabel(), status, snapshot.currency(),
			order.totalAmount(), order.paidAmount(), order.outstanding(), order.openedAt(), minutesOpen);
	}

	/** A PAID bill is a closed table; otherwise any successful payment makes it partially paid. */
	static FloorStatus statusOf(OrderView order) {
		if ("PAID".equals(order.status())) {
			return FloorStatus.CLOSED;
		}
		if (order.paidAmount() != null && order.paidAmount().signum() > 0) {
			return FloorStatus.PARTIALLY_PAID;
		}
		return FloorStatus.ACTIVE;
	}
}
