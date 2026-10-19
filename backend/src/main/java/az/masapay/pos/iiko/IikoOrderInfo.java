package az.masapay.pos.iiko;

/** Envelope iiko wraps each order in; the bill itself is in {@code order}. */
public record IikoOrderInfo(String id, IikoOrder order) {}
