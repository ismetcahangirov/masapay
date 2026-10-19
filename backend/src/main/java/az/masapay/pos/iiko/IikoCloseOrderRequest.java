package az.masapay.pos.iiko;

import java.util.UUID;

/** Body of POST /api/1/order/close. */
public record IikoCloseOrderRequest(UUID organizationId, UUID orderId) {}
