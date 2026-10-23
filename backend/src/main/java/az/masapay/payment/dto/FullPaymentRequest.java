package az.masapay.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/** Paying the whole remaining bill (#24), with an optional tip on top (#181). */
public record FullPaymentRequest(@DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal tipAmount) {
}
