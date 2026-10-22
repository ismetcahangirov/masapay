package az.masapay.order.dto;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One payment on a table's current bill, for the staff detail view (#144). */
public record TablePaymentView(UUID id, BigDecimal amount, BigDecimal tipAmount, BigDecimal totalAmount,
		BigDecimal refundedAmount, SplitType splitType, TransactionStatus status, Instant createdAt) {

	public static TablePaymentView of(Transaction transaction) {
		return new TablePaymentView(transaction.getId(), transaction.getAmount(), transaction.getTipAmount(),
			transaction.getTotalAmount(), transaction.getRefundedAmount(), transaction.getSplitType(),
			transaction.getStatus(), transaction.getCreatedAt());
	}
}
