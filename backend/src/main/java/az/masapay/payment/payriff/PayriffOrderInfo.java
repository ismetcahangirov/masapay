package az.masapay.payment.payriff;

/** The fields of GET /api/v3/orders/{orderId} we act on: whether the order was paid. */
public record PayriffOrderInfo(String orderId, String paymentStatus) {}
