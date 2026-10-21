package az.masapay.dashboard.dto;

import java.time.Instant;

/** When a paid order was opened and closed, i.e. how long the guests sat at the table. */
public record TableVisit(Instant openedAt, Instant closedAt) {
}
