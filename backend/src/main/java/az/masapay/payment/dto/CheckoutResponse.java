package az.masapay.payment.dto;

import java.util.UUID;

/** A started Payriff payment: the pending transaction and the page to send the guest to (#37). */
public record CheckoutResponse(UUID transactionId, String paymentUrl) {
}
