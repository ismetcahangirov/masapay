package az.masapay.payment.dto;

import az.masapay.domain.Order;
import az.masapay.domain.Transaction;
import java.math.BigDecimal;
import java.util.UUID;

/** Result of a settled payment. */
public record PaymentReceipt(
		UUID transactionId, UUID orderId, BigDecimal amount, String currency, String status, String orderStatus) {

	public static PaymentReceipt of(Transaction transaction, Order order) {
		return new PaymentReceipt(
			transaction.getId(), order.getId(), transaction.getTotalAmount(), transaction.getCurrency(),
			transaction.getStatus().name(), order.getStatus().name());
	}
}
