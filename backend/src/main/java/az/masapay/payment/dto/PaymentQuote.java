package az.masapay.payment.dto;

import java.math.BigDecimal;

/** The amount a payer must pay for a chosen split, shown before confirming. */
public record PaymentQuote(BigDecimal amount, String currency) {
}
