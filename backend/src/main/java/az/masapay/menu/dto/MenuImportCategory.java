package az.masapay.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A category of an imported menu; {@code existing} when the menu already has one of that name. */
public record MenuImportCategory(
		@NotBlank @Size(max = 100) String name,
		@NotEmpty @Size(max = 300) @Valid List<@NotNull MenuImportItem> items,
		boolean existing) {
}
