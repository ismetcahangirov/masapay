package az.masapay.menu;

import az.masapay.domain.MenuCategory;
import az.masapay.domain.MenuItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.menu.dto.AvailabilityRequest;
import az.masapay.menu.dto.ExtraRequest;
import az.masapay.menu.dto.ComboComponentRequest;
import az.masapay.menu.dto.MenuCategoryRequest;
import az.masapay.menu.dto.MenuCategoryView;
import az.masapay.menu.dto.MenuItemRequest;
import az.masapay.menu.dto.MenuItemView;
import az.masapay.menu.dto.SuggestionsRequest;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuComboComponentRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A restaurant's menu for the admin panel (#137). All staff read it; a manager (or super admin)
 * creates, edits and deletes categories and dishes, and only in their own restaurant. A category,
 * or dish, of another restaurant answers 404, as if it did not exist.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MenuService {

	/** Daily-menu days are Asia/Baku calendar days, like the dashboard and tip report. */
	public static final ZoneId MENU_ZONE = ZoneId.of("Asia/Baku");

	private final MenuCategoryRepository categoryRepository;
	private final MenuItemRepository itemRepository;
	private final MenuComboComponentRepository componentRepository;
	private final RestaurantRepository restaurantRepository;
	private final RestaurantTableRepository tableRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public MenuService(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
			MenuComboComponentRepository componentRepository, RestaurantRepository restaurantRepository,
			RestaurantTableRepository tableRepository, UserRepository userRepository) {
		this(categoryRepository, itemRepository, componentRepository, restaurantRepository, tableRepository,
			userRepository, Clock.systemUTC());
	}

	MenuService(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
			MenuComboComponentRepository componentRepository, RestaurantRepository restaurantRepository,
			RestaurantTableRepository tableRepository, UserRepository userRepository, Clock clock) {
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
		this.componentRepository = componentRepository;
		this.restaurantRepository = restaurantRepository;
		this.tableRepository = tableRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/** The whole menu for staff, including dishes that are off today. */
	@Transactional(readOnly = true)
	public List<MenuCategoryView> menu(UUID restaurantId, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		return menuOf(restaurantId, today(), item -> true);
	}

	/**
	 * What a customer at the table can order today (#139): dishes that are off or out of their
	 * dates are left out, and so are categories left empty.
	 */
	@Transactional(readOnly = true)
	public List<MenuCategoryView> customerMenu(UUID qrToken) {
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		LocalDate today = today();
		return menuOf(table.getRestaurant().getId(), today, item -> item.isAvailableOn(today)).stream()
			.filter(category -> !category.items().isEmpty())
			.toList();
	}

	private List<MenuCategoryView> menuOf(UUID restaurantId, LocalDate today, Predicate<MenuItem> shown) {
		Map<UUID, List<MenuItemView>> itemsByCategory = itemRepository.findByRestaurantIdOrderByNameAsc(restaurantId)
			.stream()
			.filter(shown)
			.map(item -> MenuItemView.of(item, today))
			.collect(Collectors.groupingBy(MenuItemView::categoryId));
		return categoryRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId).stream()
			.map(category -> MenuCategoryView.of(category, itemsByCategory.getOrDefault(category.getId(), List.of())))
			.toList();
	}

	/**
	 * Sets the extras offered with a dish before the customer confirms (#141). Only dishes of the
	 * same restaurant, never the dish itself, each once. A suggestion that is off today is simply
	 * not offered: the customer menu leaves it out.
	 */
	@Transactional
	public MenuItemView setSuggestions(UUID restaurantId, UUID itemId, SuggestionsRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		if (new HashSet<>(request.itemIds()).size() != request.itemIds().size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A dish is suggested twice");
		}
		if (request.itemIds().contains(itemId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A dish cannot suggest itself");
		}
		List<MenuItem> suggestions = request.itemIds().stream().map(id -> item(restaurantId, id)).toList();
		item.getSuggestions().clear();
		item.getSuggestions().addAll(suggestions);
		return MenuItemView.of(item, today());
	}

	/** Marks a dish as an extra every customer is asked about before confirming, or not (#185). */
	@Transactional
	public MenuItemView setExtra(UUID restaurantId, UUID itemId, ExtraRequest request, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		item.setAskBeforeConfirm(request.askBeforeConfirm());
		return MenuItemView.of(item, today());
	}

	/** Puts a dish on or off the daily menu (#139). */
	@Transactional
	public MenuItemView setAvailability(UUID restaurantId, UUID itemId, AvailabilityRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		LocalDate today = today();
		switch (request.mode()) {
			case ALWAYS -> setAvailability(item, true, null, null, null);
			case OFF -> setAvailability(item, false, null, null, null);
			case OFF_TODAY -> setAvailability(item, true, today, item.getAvailableFrom(), item.getAvailableUntil());
			case DATES -> {
				if (request.from() == null && request.until() == null) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give a first or a last day");
				}
				if (request.from() != null && request.until() != null && request.from().isAfter(request.until())) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The first day is after the last day");
				}
				if (request.until() != null && request.until().isBefore(today)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The last day is in the past");
				}
				setAvailability(item, true, null, request.from(), request.until());
			}
		}
		return MenuItemView.of(item, today);
	}

	private static void setAvailability(MenuItem item, boolean active, LocalDate offOn, LocalDate from,
			LocalDate until) {
		item.setActive(active);
		item.setOffOn(offOn);
		item.setAvailableFrom(from);
		item.setAvailableUntil(until);
	}

	LocalDate today() {
		return LocalDate.now(clock.withZone(MENU_ZONE));
	}

	@Transactional
	public MenuCategoryView createCategory(UUID restaurantId, MenuCategoryRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
		String name = request.name().trim();
		if (categoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
		}
		MenuCategory category = new MenuCategory();
		category.setRestaurant(restaurant);
		category.setName(name);
		category.setSortOrder(request.sortOrder());
		return MenuCategoryView.of(categoryRepository.save(category), List.of());
	}

	@Transactional
	public MenuCategoryView updateCategory(UUID restaurantId, UUID categoryId, MenuCategoryRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuCategory category = category(restaurantId, categoryId);
		String name = request.name().trim();
		if (categoryRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(restaurantId, name, categoryId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
		}
		category.setName(name);
		category.setSortOrder(request.sortOrder());
		return MenuCategoryView.of(category, List.of());
	}

	/** A category that still has dishes is not deleted: the manager moves or deletes them first. */
	@Transactional
	public void deleteCategory(UUID restaurantId, UUID categoryId, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuCategory category = category(restaurantId, categoryId);
		if (itemRepository.existsByCategoryId(categoryId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The category still has dishes");
		}
		categoryRepository.delete(category);
	}

	@Transactional
	public MenuItemView createItem(UUID restaurantId, MenuItemRequest request, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuCategory category = category(restaurantId, request.categoryId());
		Map<MenuItem, Integer> components = components(restaurantId, null, request.components());
		MenuItem item = new MenuItem();
		item.setRestaurant(category.getRestaurant());
		apply(item, category, request);
		item.replaceComponents(components);
		return MenuItemView.of(itemRepository.save(item), today());
	}

	@Transactional
	public MenuItemView updateItem(UUID restaurantId, UUID itemId, MenuItemRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		Map<MenuItem, Integer> components = components(restaurantId, item, request.components());
		apply(item, category(restaurantId, request.categoryId()), request);
		item.replaceComponents(components);
		return MenuItemView.of(item, today());
	}

	/** A dish that is part of a combo stays until the combo no longer contains it. */
	@Transactional
	public void deleteItem(UUID restaurantId, UUID itemId, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		if (componentRepository.existsByComponentId(itemId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The dish is part of a combo");
		}
		itemRepository.delete(item);
	}

	private static void apply(MenuItem item, MenuCategory category, MenuItemRequest request) {
		item.setCategory(category);
		item.setName(request.name().trim());
		item.setDescription(blankToNull(request.description()));
		item.setIngredients(blankToNull(request.ingredients()));
		item.setPrice(request.price());
	}

	static String blankToNull(String text) {
		String trimmed = text == null ? null : text.trim();
		return trimmed == null || trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * Resolves a combo's contents. Only one level is allowed: a combo holds plain dishes of the
	 * same restaurant, never itself or another combo, and a dish already inside a combo cannot
	 * become a combo. {@code self} is the item being edited, null on create.
	 */
	private Map<MenuItem, Integer> components(UUID restaurantId, MenuItem self, List<ComboComponentRequest> wanted) {
		if (wanted == null || wanted.isEmpty()) {
			return Map.of();
		}
		if (self != null && componentRepository.existsByComponentId(self.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"A dish inside a combo cannot become a combo");
		}
		Map<MenuItem, Integer> components = new LinkedHashMap<>();
		Set<UUID> seen = new HashSet<>();
		for (ComboComponentRequest row : wanted) {
			if (!seen.add(row.itemId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A dish is listed twice in the combo");
			}
			if (self != null && row.itemId().equals(self.getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A combo cannot contain itself");
			}
			MenuItem dish = item(restaurantId, row.itemId());
			if (dish.isCombo()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A combo cannot contain another combo");
			}
			components.put(dish, row.quantity());
		}
		return components;
	}

	private MenuCategory category(UUID restaurantId, UUID categoryId) {
		return categoryRepository.findById(categoryId)
			.filter(category -> category.getRestaurant().getId().equals(restaurantId))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
	}

	MenuItem item(UUID restaurantId, UUID itemId) {
		return itemRepository.findById(itemId)
			.filter(item -> item.getRestaurant().getId().equals(restaurantId))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
	}

	void checkManager(AuthenticatedUser caller, UUID restaurantId) {
		if (caller.role() == UserRole.WAITER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiters cannot change the menu");
		}
		checkSameRestaurant(caller, restaurantId);
	}

	/** Staff may only touch their own restaurant's menu; a super admin any. */
	private void checkSameRestaurant(AuthenticatedUser caller, UUID restaurantId) {
		if (caller.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		User user = userRepository.findById(caller.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(restaurantId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
	}
}
