package az.masapay.payment.refund;

import az.masapay.domain.Transaction;
import java.math.BigDecimal;
import java.util.UUID;

/** Outcome of a refund as shown to the admin panel. */
public record RefundResult(UUID transactionId, String status, BigDecimal refundedAmount, String currency) {

	public static RefundResult of(Transaction transaction) {
		return new RefundResult(transaction.getId(), transaction.getStatus().name(),
				transaction.getRefundedAmount(), transaction.getCurrency());
	}
}
