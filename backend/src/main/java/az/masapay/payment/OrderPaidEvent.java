package az.masapay.payment;

import java.util.UUID;

/** Published when an order is fully paid; listeners run after the payment transaction commits. */
public record OrderPaidEvent(UUID orderId) {}
