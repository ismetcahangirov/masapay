package az.masapay.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * One dish of an imported menu, as guessed from the file and then as the manager approved it.
 * {@code image} is a {@code data:} URL or null. {@code existingId} is the dish of the same name
 * already on the menu, which the import updates instead of adding a second one; the server
 * works it out again on save, so a client cannot point it at another dish. {@code ingredients}
 * are the main ingredients, comma separated.
 */
public record MenuImportItem(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 500) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@Size(max = 3_000_000) String image,
		UUID existingId,
		@Size(max = 300) String ingredients) {

	/** Without ingredients. */
	public MenuImportItem(String name, String description, BigDecimal price, String image, UUID existingId) {
		this(name, description, price, image, existingId, null);
	}
}
