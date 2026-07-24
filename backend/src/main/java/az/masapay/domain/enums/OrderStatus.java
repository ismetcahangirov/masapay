package az.masapay.domain.enums;

/** Lifecycle state of an order (adisyon). Matches the orders.status CHECK constraint. */
public enum OrderStatus {
	OPEN,
	CLOSED,
	PAID,
	CANCELLED
}
