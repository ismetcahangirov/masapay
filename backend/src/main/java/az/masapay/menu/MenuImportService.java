package az.masapay.menu;

import az.masapay.domain.MenuCategory;
import az.masapay.domain.MenuItem;
import az.masapay.domain.Restaurant;
import az.masapay.menu.dto.MenuImportCategory;
import az.masapay.menu.dto.MenuImportDraft;
import az.masapay.menu.dto.MenuImportItem;
import az.masapay.menu.dto.MenuImportRequest;
import az.masapay.menu.dto.MenuImportResult;
import az.masapay.menu.importing.MenuDocumentReader;
import az.masapay.menu.importing.MenuOcr;
import az.masapay.menu.importing.MenuTextParser;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A whole menu from a PDF or Word file, or a photo or scan of a printed one (read with OCR), in
 * two steps. First the file is read and guessed into
 * categories and dishes, and nothing is saved: the manager reviews it, fixes names and prices,
 * adds photos. Then the approved list is saved at once. A category or dish whose name is already
 * on the menu (any letter case) is reused and updated, so importing a new price list twice does
 * not double the menu. Dishes added one by one keep working as before.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MenuImportService {

	private static final Locale AZ = Locale.forLanguageTag("az");

	private final MenuService menuService;
	private final MenuImageService imageService;
	private final MenuCategoryRepository categoryRepository;
	private final MenuItemRepository itemRepository;
	private final RestaurantRepository restaurantRepository;
	private final MenuOcr ocr;

	public MenuImportService(MenuService menuService, MenuImageService imageService,
			MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
			RestaurantRepository restaurantRepository, MenuOcr ocr) {
		this.menuService = menuService;
		this.imageService = imageService;
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
		this.restaurantRepository = restaurantRepository;
		this.ocr = ocr;
	}

	/**
	 * The file as a menu to review; 422 NO_DISHES when no line looked like a dish with a price.
	 * Not one transaction: OCR of a scan can take a minute, and no database connection should
	 * wait for it. Each lookup below reads on its own.
	 */
	public MenuImportDraft preview(UUID restaurantId, byte[] file, AuthenticatedUser caller) {
		menuService.checkManager(caller, restaurantId);
		MenuDocumentReader.Read read = MenuDocumentReader.read(file, ocr);
		List<MenuTextParser.Category> parsed = MenuTextParser.parse(read.lines());
		if (parsed.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_DISHES");
		}
		Set<String> categoryNames = new HashSet<>();
		categoryRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId)
			.forEach(category -> categoryNames.add(key(category.getName())));
		Map<String, MenuItem> dishes = dishesByName(restaurantId);
		int skippedImages = 0;
		List<MenuImportCategory> categories = new ArrayList<>();
		for (MenuTextParser.Category category : parsed) {
			List<MenuImportItem> items = new ArrayList<>();
			for (MenuTextParser.Dish dish : category.dishes()) {
				String image = null;
				if (dish.picture() != null) {
					MenuImages.Image photo = MenuImages.normalise(dish.picture());
					if (photo == null) {
						skippedImages++;
					} else {
						image = photo.dataUrl();
					}
				}
				String name = cut(dish.name(), 150);
				MenuItem existing = dishes.get(key(name));
				items.add(new MenuImportItem(name, cut(dish.description(), 500), dish.price(), image,
					existing == null ? null : existing.getId(), cut(dish.ingredients(), 300)));
			}
			String name = cut(category.name(), 100);
			categories.add(new MenuImportCategory(name, items, categoryNames.contains(key(name))));
		}
		return new MenuImportDraft(categories, skippedImages, read.ocr());
	}

	/** Saves the approved menu; one dish name twice answers 400 DUPLICATE_DISH and nothing is saved. */
	@Transactional
	public MenuImportResult apply(UUID restaurantId, MenuImportRequest request, AuthenticatedUser caller) {
		menuService.checkManager(caller, restaurantId);
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
		Set<String> seen = new HashSet<>();
		for (MenuImportCategory category : request.categories()) {
			for (MenuImportItem item : category.items()) {
				if (!seen.add(key(item.name()))) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DUPLICATE_DISH");
				}
			}
		}

		List<MenuCategory> existingCategories = categoryRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurantId);
		Map<String, MenuCategory> categories = new HashMap<>();
		existingCategories.forEach(category -> categories.putIfAbsent(key(category.getName()), category));
		int nextSort = existingCategories.stream().mapToInt(MenuCategory::getSortOrder).max().orElse(-1) + 1;
		Map<String, MenuItem> dishes = dishesByName(restaurantId);
		int categoriesCreated = 0;
		int created = 0;
		int updated = 0;

		for (MenuImportCategory wanted : request.categories()) {
			String categoryName = wanted.name().trim();
			MenuCategory category = categories.get(key(categoryName));
			if (category == null) {
				category = new MenuCategory();
				category.setRestaurant(restaurant);
				category.setName(categoryName);
				category.setSortOrder(nextSort++);
				category = categoryRepository.save(category);
				categories.put(key(categoryName), category);
				categoriesCreated++;
			}
			for (MenuImportItem wantedDish : wanted.items()) {
				MenuImages.Image photo = null;
				if (wantedDish.image() != null && !wantedDish.image().isBlank()) {
					photo = MenuImages.fromDataUrl(wantedDish.image());
					if (photo == null) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE");
					}
				}
				String name = wantedDish.name().trim();
				MenuItem dish = dishes.get(key(name));
				if (dish == null) {
					dish = new MenuItem();
					dish.setRestaurant(restaurant);
					created++;
				} else {
					updated++;
				}
				dish.setCategory(category);
				dish.setName(name);
				dish.setDescription(MenuService.blankToNull(wantedDish.description()));
				dish.setIngredients(MenuService.blankToNull(wantedDish.ingredients()));
				dish.setPrice(wantedDish.price());
				dish = itemRepository.save(dish);
				if (photo != null) {
					imageService.store(dish, photo);
				}
			}
		}
		return new MenuImportResult(categoriesCreated, created, updated);
	}

	private Map<String, MenuItem> dishesByName(UUID restaurantId) {
		Map<String, MenuItem> dishes = new HashMap<>();
		itemRepository.findByRestaurantIdOrderByNameAsc(restaurantId)
			.forEach(item -> dishes.putIfAbsent(key(item.getName()), item));
		return dishes;
	}

	private static String key(String name) {
		return name.trim().toLowerCase(AZ);
	}

	private static String cut(String text, int max) {
		return text == null || text.length() <= max ? text : text.substring(0, max).trim();
	}
}
