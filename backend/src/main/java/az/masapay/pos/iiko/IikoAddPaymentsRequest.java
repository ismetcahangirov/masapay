package az.masapay.pos.iiko;

import java.util.List;
import java.util.UUID;

/** Body of POST /api/1/order/add_payments. */
public record IikoAddPaymentsRequest(UUID organizationId, UUID orderId, List<IikoPayment> payments) {}
