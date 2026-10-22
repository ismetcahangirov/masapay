package az.masapay.domain.enums;

/** Where an ordered dish is in the kitchen (#142); matches the order_items.kitchen_status CHECK. */
public enum KitchenStatus {
	/** Ordered, not started yet. */
	NEW,
	PREPARING,
	READY
}
