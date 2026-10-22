package az.masapay.menu;

import az.masapay.domain.MenuCategory;
import az.masapay.domain.MenuItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.menu.dto.ComboComponentRequest;
import az.masapay.menu.dto.MenuCategoryRequest;
import az.masapay.menu.dto.MenuCategoryView;
import az.masapay.menu.dto.MenuItemRequest;
import az.masapay.menu.dto.MenuItemView;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuComboComponentRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

	private final MenuCategoryRepository categoryRepository;
	private final MenuItemRepository itemRepository;
	private final MenuComboComponentRepository componentRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;

	public MenuService(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
			MenuComboComponentRepository componentRepository, RestaurantRepository restaurantRepository,
			UserRepository userRepository) {
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
		this.componentRepository = componentRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<MenuCategoryView> menu(UUID restaurantId, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		Map<UUID, List<MenuItemView>> itemsByCategory = itemRepository.findByRestaurantIdOrderByNameAsc(restaurantId)
			.stream()
			.map(MenuItemView::of)
			.collect(Collectors.groupingBy(MenuItemView::categoryId));
		return categoryRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId).stream()
			.map(category -> MenuCategoryView.of(category, itemsByCategory.getOrDefault(category.getId(), List.of())))
			.toList();
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
		return MenuItemView.of(itemRepository.save(item));
	}

	@Transactional
	public MenuItemView updateItem(UUID restaurantId, UUID itemId, MenuItemRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		Map<MenuItem, Integer> components = components(restaurantId, item, request.components());
		apply(item, category(restaurantId, request.categoryId()), request);
		item.replaceComponents(components);
		return MenuItemView.of(item);
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
		String description = request.description() == null ? null : request.description().trim();
		item.setCategory(category);
		item.setName(request.name().trim());
		item.setDescription(description == null || description.isEmpty() ? null : description);
		item.setPrice(request.price());
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

	private MenuItem item(UUID restaurantId, UUID itemId) {
		return itemRepository.findById(itemId)
			.filter(item -> item.getRestaurant().getId().equals(restaurantId))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
	}

	private void checkManager(AuthenticatedUser caller, UUID restaurantId) {
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
