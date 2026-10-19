package az.masapay.pos.iiko;

import java.util.List;

/** Response of POST /api/1/order/by_id. */
public record IikoOrdersResponse(List<IikoOrderInfo> orders) {}
