package az.masapay.payment.payriff;

/** Body Payriff posts to our callback URL: the order's current state inside the usual envelope. */
public record PayriffCallback(Payload payload) {

	/** The fields of Payriff's order info we act on; the rest is ignored. */
	public record Payload(String orderId, String paymentStatus) {}
}
