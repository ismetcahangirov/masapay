package az.masapay.platform.dto;

import az.masapay.domain.enums.SubscriptionPlan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Set or extend a restaurant's subscription (#148): plan, whether it is a trial, first and last
 * day (both included, Baku time) and the monthly fee in the restaurant's currency.
 */
public record SubscriptionRequest(
		@NotNull SubscriptionPlan plan,
		boolean trial,
		@NotNull LocalDate startsOn,
		@NotNull LocalDate endsOn,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal monthlyFee) {
}
