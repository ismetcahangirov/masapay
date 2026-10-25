package az.masapay.payment.payriff;

/**
 * Payriff did not do what was asked. Either it refused for a business reason (a {@code 01xxx}
 * result code, e.g. "Refund time expired."), or it could not be reached or answered with an
 * error of its own (5xx, a timeout, {@code 15000 Internal Error}). Callers decide what the guest
 * or the manager is told; {@link #refused()} tells the two apart.
 */
public class PayriffException extends IllegalStateException {

	/** Payriff's own result codes for a business refusal start with this. */
	private static final String REFUSAL_PREFIX = "01";

	private final int httpStatus;
	private final String code;
	private final String payriffMessage;

	public PayriffException(String operation, int httpStatus, String code, String payriffMessage) {
		super("Payriff " + operation + " failed: HTTP " + httpStatus + " " + code + " " + payriffMessage);
		this.httpStatus = httpStatus;
		this.code = code;
		this.payriffMessage = payriffMessage;
	}

	/** Payriff answered and said no (the request reached it and was judged). */
	public boolean refused() {
		return code != null && code.startsWith(REFUSAL_PREFIX);
	}

	/** HTTP status Payriff answered with; 0 when it could not be reached at all. */
	public int httpStatus() {
		return httpStatus;
	}

	public String code() {
		return code;
	}

	/** Payriff's own wording, e.g. "Refund time expired.\n Support ticket: 840003". */
	public String payriffMessage() {
		return payriffMessage;
	}
}
