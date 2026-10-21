package az.masapay.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** A line a waiter (or a POS bridge) adds to a table's bill (#21). */
public record AddOrderItemRequest(
		@NotBlank @Size(max = 255) String name,
		@NotNull @DecimalMin(value = "0.001") @Digits(integer = 7, fraction = 3) BigDecimal quantity,
		@NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {
}
