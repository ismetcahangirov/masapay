package az.masapay.transaction;

import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Search criteria of the admin transactions table (#45). Every field except the restaurant is
 * optional; {@code createdFrom} is inclusive and {@code createdTo} exclusive.
 */
record TransactionFilter(UUID restaurantId, TransactionStatus status, SplitType splitType,
	Instant createdFrom, Instant createdTo, String query) {
}
