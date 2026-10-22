package az.masapay.menu.dto;

import az.masapay.domain.MenuItem;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemView(UUID id, UUID categoryId, String name, String description, BigDecimal price) {

	public static MenuItemView of(MenuItem item) {
		return new MenuItemView(item.getId(), item.getCategory().getId(), item.getName(), item.getDescription(),
			item.getPrice());
	}
}
