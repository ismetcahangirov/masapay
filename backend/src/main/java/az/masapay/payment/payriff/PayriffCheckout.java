package az.masapay.payment.payriff;

/** What the customer needs to pay: the Payriff order and the page to send them to. */
public record PayriffCheckout(String payriffOrderId, String paymentUrl) {}
