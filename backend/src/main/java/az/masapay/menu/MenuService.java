package az.masapay.menu;

import az.masapay.domain.MenuCategory;
import az.masapay.domain.MenuItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.menu.dto.MenuCategoryRequest;
import az.masapay.menu.dto.MenuCategoryView;
import az.masapay.menu.dto.MenuItemRequest;
import az.masapay.menu.dto.MenuItemView;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
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
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;

	public MenuService(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
			RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
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
		MenuItem item = new MenuItem();
		item.setRestaurant(category.getRestaurant());
		apply(item, category, request);
		return MenuItemView.of(itemRepository.save(item));
	}

	@Transactional
	public MenuItemView updateItem(UUID restaurantId, UUID itemId, MenuItemRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		MenuItem item = item(restaurantId, itemId);
		apply(item, category(restaurantId, request.categoryId()), request);
		return MenuItemView.of(item);
	}

	@Transactional
	public void deleteItem(UUID restaurantId, UUID itemId, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		itemRepository.delete(item(restaurantId, itemId));
	}

	private static void apply(MenuItem item, MenuCategory category, MenuItemRequest request) {
		String description = request.description() == null ? null : request.description().trim();
		item.setCategory(category);
		item.setName(request.name().trim());
		item.setDescription(description == null || description.isEmpty() ? null : description);
		item.setPrice(request.price());
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
