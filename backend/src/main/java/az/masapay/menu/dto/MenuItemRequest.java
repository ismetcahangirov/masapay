package az.masapay.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Create or edit a dish. The price is in manat with at most two decimals, like the DB column.
 * A non-empty {@code components} list makes it a combo such as "Biznes lanç" (#138); null or
 * empty is an ordinary dish. The list replaces the combo's contents on every save.
 */
public record MenuItemRequest(
		@NotNull UUID categoryId,
		@NotBlank @Size(max = 150) String name,
		@Size(max = 500) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@Valid @Size(max = 20) List<@NotNull ComboComponentRequest> components) {

	/** An ordinary dish, without combo contents. */
	public MenuItemRequest(UUID categoryId, String name, String description, BigDecimal price) {
		this(categoryId, name, description, price, null);
	}
}
