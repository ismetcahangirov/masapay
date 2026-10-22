package az.masapay.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Create or edit a dish. The price is in manat with at most two decimals, like the DB column. */
public record MenuItemRequest(
		@NotNull UUID categoryId,
		@NotBlank @Size(max = 150) String name,
		@Size(max = 500) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price) {
}
