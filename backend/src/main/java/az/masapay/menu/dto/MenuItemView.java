package az.masapay.menu.dto;

import az.masapay.domain.MenuItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A dish; {@code components} lists a combo's contents and is empty for an ordinary dish (#138). */
public record MenuItemView(UUID id, UUID categoryId, String name, String description, BigDecimal price,
		List<ComboComponentView> components) {

	public static MenuItemView of(MenuItem item) {
		return new MenuItemView(item.getId(), item.getCategory().getId(), item.getName(), item.getDescription(),
			item.getPrice(), item.getComponents().stream().map(ComboComponentView::of).toList());
	}
}
