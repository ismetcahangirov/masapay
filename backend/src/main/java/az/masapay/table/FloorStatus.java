package az.masapay.table;

/** How a table looks on the live floor plan (#44). */
public enum FloorStatus {
	/** No open bill. */
	EMPTY,
	/** Open bill, nothing paid yet. */
	ACTIVE,
	/** Open bill with part of it already paid. */
	PARTIALLY_PAID,
	/** Bill fully paid, still inside the receipt window. */
	CLOSED
}
