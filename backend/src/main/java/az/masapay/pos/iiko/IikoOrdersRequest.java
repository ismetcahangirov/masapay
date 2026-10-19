package az.masapay.pos.iiko;

import java.util.List;
import java.util.UUID;

/** Body of POST /api/1/order/by_id. */
public record IikoOrdersRequest(List<UUID> organizationIds, List<UUID> orderIds) {}
