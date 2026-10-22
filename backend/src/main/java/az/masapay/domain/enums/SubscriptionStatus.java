package az.masapay.domain.enums;

/** Where a restaurant's subscription stands on a given day (#148). */
public enum SubscriptionStatus {
	/** No subscription was ever set; nothing is blocked. */
	NONE,
	TRIAL,
	ACTIVE,
	/** The end date has passed: customers cannot start payments. */
	EXPIRED
}
