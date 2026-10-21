package az.masapay.payment.payriff;

import java.math.BigDecimal;

/** Body of POST /api/v3/refund: returns money of a paid Payriff order to the card. */
public record PayriffRefundRequest(String orderId, BigDecimal amount, String refundReason) {}
