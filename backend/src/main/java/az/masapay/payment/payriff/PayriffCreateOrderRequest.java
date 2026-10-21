package az.masapay.payment.payriff;

import java.math.BigDecimal;

/** Body of POST /api/v3/orders: opens a Payriff payment page for this amount. */
public record PayriffCreateOrderRequest(
		BigDecimal amount,
		String currency,
		String operation,
		String language,
		String description,
		String callbackUrl,
		String redirectUrl) {}
