package az.masapay.table.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * A table's own service charge (#146): null follows the restaurant again, 0 charges none at this
 * table, anything else is this table's percentage.
 */
public record TableServiceChargeRequest(
		@DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal percent) {
}
