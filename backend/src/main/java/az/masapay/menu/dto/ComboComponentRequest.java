package az.masapay.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** A dish of the same restaurant and how many of it go into the combo. */
public record ComboComponentRequest(@NotNull UUID itemId, @NotNull @Min(1) @Max(99) Integer quantity) {
}
