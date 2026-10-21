package az.masapay.payment.dto;

import az.masapay.domain.Transaction;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * What the payment result page shows after Payriff sends the guest back (#37): the payment's
 * status and where the guest's table is, so the page can link back to the bill.
 */
public record PaymentStatusView(
		UUID transactionId, String status, BigDecimal amount, String currency, String orderStatus,
		UUID restaurantId, UUID tableId, UUID qrToken) {

	public static PaymentStatusView of(Transaction transaction) {
		var table = transaction.getOrder().getTable();
		return new PaymentStatusView(
			transaction.getId(), transaction.getStatus().name(), transaction.getTotalAmount(),
			transaction.getCurrency(), transaction.getOrder().getStatus().name(),
			transaction.getRestaurant().getId(), table.getId(), table.getQrToken());
	}
}
