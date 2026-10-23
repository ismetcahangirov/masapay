package az.masapay.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The line items a diner selected to pay for (item-based split, #25).
 * {@code guests} is how many people share the bill's service charge; it is needed
 * only when the bill has one and nobody has fixed the guest count yet. {@code tipAmount} is an
 * optional tip on top (#181).
 */
public record ItemPaymentRequest(
		@NotEmpty List<UUID> itemIds, @Min(1) @Max(EqualPaymentRequest.MAX_PARTS) Integer guests,
		@DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal tipAmount) {
}
