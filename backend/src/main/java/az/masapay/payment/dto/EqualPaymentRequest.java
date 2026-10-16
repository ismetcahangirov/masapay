package az.masapay.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Equal split (#26): the bill is divided into {@code parts} equal shares and this
 * payer covers {@code partsToPay} of them (e.g. one guest paying for two seats).
 */
public record EqualPaymentRequest(
		@Min(EqualPaymentRequest.MIN_PARTS) @Max(EqualPaymentRequest.MAX_PARTS) int parts,
		@Min(1) @Max(EqualPaymentRequest.MAX_PARTS) int partsToPay) {

	public static final int MIN_PARTS = 2;
	public static final int MAX_PARTS = 50;
}
