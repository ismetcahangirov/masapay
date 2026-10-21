package az.masapay.transaction.dto;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One row of the admin transactions table (#45). */
public record TransactionRow(UUID id, Instant createdAt, Instant paidAt, String tableLabel, SplitType splitType,
	TransactionStatus status, BigDecimal amount, BigDecimal tipAmount, BigDecimal totalAmount,
	BigDecimal refundedAmount, String currency, String payriffOrderId, boolean refundable) {

	public static TransactionRow of(Transaction transaction) {
		return new TransactionRow(transaction.getId(), transaction.getCreatedAt(), transaction.getPaidAt(),
			transaction.getOrder().getTable().getLabel(), transaction.getSplitType(), transaction.getStatus(),
			transaction.getAmount(), transaction.getTipAmount(), transaction.getTotalAmount(),
			transaction.getRefundedAmount(), transaction.getCurrency(), transaction.getPayriffOrderId(),
			isRefundable(transaction));
	}

	/** Same rule as the refund endpoint: only a successful payment that went through Payriff. */
	public static boolean isRefundable(Transaction transaction) {
		return transaction.getStatus() == TransactionStatus.SUCCESS && transaction.getPayriffOrderId() != null;
	}
}
