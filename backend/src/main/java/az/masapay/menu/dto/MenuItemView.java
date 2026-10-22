package az.masapay.menu.dto;

import az.masapay.domain.MenuItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A dish; {@code components} lists a combo's contents and is empty for an ordinary dish (#138).
 * The availability fields are the manager's settings and {@code availableToday} what customers
 * see right now (#139).
 */
public record MenuItemView(UUID id, UUID categoryId, String name, String description, BigDecimal price,
		List<ComboComponentView> components, boolean active, LocalDate offOn, LocalDate availableFrom,
		LocalDate availableUntil, boolean availableToday) {

	public static MenuItemView of(MenuItem item, LocalDate today) {
		return new MenuItemView(item.getId(), item.getCategory().getId(), item.getName(), item.getDescription(),
			item.getPrice(), item.getComponents().stream().map(ComboComponentView::of).toList(), item.isActive(),
			item.getOffOn(), item.getAvailableFrom(), item.getAvailableUntil(), item.isAvailableOn(today));
	}
}
