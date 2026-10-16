package az.masapay.payment.dto;

import java.math.BigDecimal;

/**
 * The amount for the requested equal shares, plus the split's progress so the UI
 * can show how many parts are already paid.
 */
public record EqualPaymentQuote(BigDecimal amount, String currency, int totalParts, int paidParts) {
}
