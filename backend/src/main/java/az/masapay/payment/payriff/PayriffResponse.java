package az.masapay.payment.payriff;

/**
 * Envelope Payriff wraps every response in. Payriff can report a failure with HTTP 200, so the
 * {@code code} decides: {@value #SUCCESS_CODE} means success.
 */
public record PayriffResponse<T>(String code, String message, T payload) {

	public static final String SUCCESS_CODE = "00000";

	public boolean succeeded() {
		return SUCCESS_CODE.equals(code);
	}
}
