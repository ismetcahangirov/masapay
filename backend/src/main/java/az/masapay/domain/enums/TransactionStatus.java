package az.masapay.domain.enums;

/** Payment state of a transaction. Matches the transactions.status CHECK constraint. */
public enum TransactionStatus {
	PENDING,
	SUCCESS,
	FAILED,
	REFUNDED
}
