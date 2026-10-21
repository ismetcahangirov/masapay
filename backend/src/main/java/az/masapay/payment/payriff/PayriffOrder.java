package az.masapay.payment.payriff;

/** Payload of a created Payriff order; the customer pays on {@code paymentUrl}. */
public record PayriffOrder(String orderId, String sessionId, String paymentUrl, Long transactionId) {}
