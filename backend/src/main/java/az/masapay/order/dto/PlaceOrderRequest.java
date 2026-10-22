package az.masapay.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * A customer's confirmed basket (#140): menu dishes and how many of each. Prices are never sent;
 * the server takes them from the menu.
 */
public record PlaceOrderRequest(@NotEmpty @Size(max = 30) List<@Valid @NotNull Line> items) {

	public record Line(@NotNull UUID menuItemId, @NotNull @Min(1) @Max(20) Integer quantity) {
	}
}
