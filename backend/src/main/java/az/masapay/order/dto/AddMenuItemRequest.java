package az.masapay.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Staff add a dish from the menu to a table's bill (#144); the price comes from the menu. */
public record AddMenuItemRequest(@NotNull UUID menuItemId, @NotNull @Min(1) @Max(20) Integer quantity) {
}
