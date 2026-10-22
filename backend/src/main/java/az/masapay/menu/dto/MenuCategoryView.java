package az.masapay.menu.dto;

import az.masapay.domain.MenuCategory;
import java.util.List;
import java.util.UUID;

/** A category with its dishes, as the admin menu page (and later the customer menu) shows it. */
public record MenuCategoryView(UUID id, String name, int sortOrder, List<MenuItemView> items) {

	public static MenuCategoryView of(MenuCategory category, List<MenuItemView> items) {
		return new MenuCategoryView(category.getId(), category.getName(), category.getSortOrder(), items);
	}
}
