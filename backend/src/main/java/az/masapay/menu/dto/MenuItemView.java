package az.masapay.menu.dto;

import az.masapay.domain.MenuItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A dish; {@code components} lists a combo's contents and is empty for an ordinary dish (#138).
 * The availability fields are the manager's settings and {@code availableToday} what customers
 * see right now (#139). {@code suggestionIds} are the extras offered with it, in order (#141).
 * {@code askBeforeConfirm} marks an extra every customer is asked about before confirming (#185).
 * {@code imageUrl} is the dish photo, or null; it changes with every new photo.
 * {@code ingredients} are the main ingredients, comma separated, or null.
 */
public record MenuItemView(UUID id, UUID categoryId, String name, String description, BigDecimal price,
		List<ComboComponentView> components, boolean active, LocalDate offOn, LocalDate availableFrom,
		LocalDate availableUntil, boolean availableToday, List<UUID> suggestionIds, boolean askBeforeConfirm,
		String imageUrl, String ingredients) {

	public static MenuItemView of(MenuItem item, LocalDate today) {
		return new MenuItemView(item.getId(), item.getCategory().getId(), item.getName(), item.getDescription(),
			item.getPrice(), item.getComponents().stream().map(ComboComponentView::of).toList(), item.isActive(),
			item.getOffOn(), item.getAvailableFrom(), item.getAvailableUntil(), item.isAvailableOn(today),
			item.getSuggestions().stream().map(MenuItem::getId).toList(), item.isAskBeforeConfirm(),
			imageUrl(item), item.getIngredients());
	}

	/** Public, versioned photo URL, so the browser may cache it for good. */
	public static String imageUrl(MenuItem item) {
		return item.getImageVersion() > 0
			? "/api/public/menu-images/" + item.getId() + "?v=" + item.getImageVersion()
			: null;
	}
}
