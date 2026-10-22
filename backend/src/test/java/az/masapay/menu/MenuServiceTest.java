package az.masapay.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MenuServiceTest {

	private final MenuCategoryRepository categoryRepository = mock(MenuCategoryRepository.class);
	private final MenuItemRepository itemRepository = mock(MenuItemRepository.class);
	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final MenuService service = new MenuService(categoryRepository, itemRepository, restaurantRepository,
			userRepository);

	private final Restaurant restaurant = new Restaurant();
	private final Restaurant otherRestaurant = new Restaurant();
	private final AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);

	@BeforeEach
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		otherRestaurant.setId(UUID.randomUUID());
		User staff = new User();
		staff.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(staff));
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(staff));
		when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
		when(categoryRepository.save(any(MenuCategory.class))).thenAnswer(inv -> {
			MenuCategory category = inv.getArgument(0);
			category.setId(UUID.randomUUID());
			return category;
		});
		when(itemRepository.save(any(MenuItem.class))).thenAnswer(inv -> {
			MenuItem item = inv.getArgument(0);
			item.setId(UUID.randomUUID());
			return item;
		});
	}

	private MenuCategory category(Restaurant owner, String name, int sortOrder) {
		MenuCategory category = new MenuCategory();
		category.setId(UUID.randomUUID());
		category.setRestaurant(owner);
		category.setName(name);
		category.setSortOrder(sortOrder);
		when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
		return category;
	}

	private MenuItem item(MenuCategory category, String name) {
		MenuItem item = new MenuItem();
		item.setId(UUID.randomUUID());
		item.setRestaurant(category.getRestaurant());
		item.setCategory(category);
		item.setName(name);
		item.setPrice(new BigDecimal("5.00"));
		when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
		return item;
	}

	@Test
	void aWaiterReadsTheMenuWithDishesUnderTheirCategory() {
		MenuCategory salads = category(restaurant, "Salatlar", 0);
		MenuCategory drinks = category(restaurant, "İçkilər", 1);
		when(categoryRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurant.getId()))
			.thenReturn(List.of(salads, drinks));
		List<MenuItem> dishes = List.of(item(salads, "Çoban"), item(salads, "Sezar"));
		when(itemRepository.findByRestaurantIdOrderByNameAsc(restaurant.getId())).thenReturn(dishes);

		List<MenuCategoryView> menu = service.menu(restaurant.getId(), waiter);

		assertThat(menu).extracting(MenuCategoryView::name).containsExactly("Salatlar", "İçkilər");
		assertThat(menu.get(0).items()).extracting(MenuItemView::name).containsExactly("Çoban", "Sezar");
		assertThat(menu.get(1).items()).isEmpty();
	}

	@Test
	void staffOfAnotherRestaurantCannotReadTheMenu() {
		assertThatThrownBy(() -> service.menu(otherRestaurant.getId(), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void aManagerCreatesACategoryWithATrimmedName() {
		MenuCategoryView created = service.createCategory(restaurant.getId(),
				new MenuCategoryRequest("  Salatlar ", 2), manager);

		assertThat(created.name()).isEqualTo("Salatlar");
		assertThat(created.sortOrder()).isEqualTo(2);
		assertThat(created.items()).isEmpty();
	}

	@Test
	void aWaiterCannotCreateACategory() {
		assertThatThrownBy(() -> service.createCategory(restaurant.getId(), new MenuCategoryRequest("X", 0), waiter))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void aDuplicateCategoryNameIsAConflict() {
		when(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurant.getId(), "Salatlar"))
			.thenReturn(true);

		assertThatThrownBy(() -> service.createCategory(restaurant.getId(),
				new MenuCategoryRequest("Salatlar", 0), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void aManagerRenamesAndReordersACategory() {
		MenuCategory salads = category(restaurant, "Salatlar", 0);

		MenuCategoryView updated = service.updateCategory(restaurant.getId(), salads.getId(),
				new MenuCategoryRequest("Soyuq salatlar", 5), manager);

		assertThat(updated.name()).isEqualTo("Soyuq salatlar");
		assertThat(salads.getSortOrder()).isEqualTo(5);
	}

	@Test
	void aCategoryOfAnotherRestaurantIsNotFound() {
		MenuCategory foreign = category(otherRestaurant, "Salatlar", 0);

		assertThatThrownBy(() -> service.updateCategory(restaurant.getId(), foreign.getId(),
				new MenuCategoryRequest("Mine now", 0), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		assertThat(foreign.getName()).isEqualTo("Salatlar");
	}

	@Test
	void aCategoryWithDishesIsNotDeleted() {
		MenuCategory salads = category(restaurant, "Salatlar", 0);
		when(itemRepository.existsByCategoryId(salads.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.deleteCategory(restaurant.getId(), salads.getId(), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(categoryRepository, never()).delete(any());
	}

	@Test
	void anEmptyCategoryIsDeleted() {
		MenuCategory salads = category(restaurant, "Salatlar", 0);

		service.deleteCategory(restaurant.getId(), salads.getId(), manager);

		verify(categoryRepository).delete(salads);
	}

	@Test
	void aManagerAddsADishAndABlankDescriptionIsStoredAsNull() {
		MenuCategory salads = category(restaurant, "Salatlar", 0);

		MenuItemView created = service.createItem(restaurant.getId(),
				new MenuItemRequest(salads.getId(), " Sezar ", "   ", new BigDecimal("8.50")), manager);

		assertThat(created.name()).isEqualTo("Sezar");
		assertThat(created.description()).isNull();
		assertThat(created.categoryId()).isEqualTo(salads.getId());
		assertThat(created.price()).isEqualByComparingTo("8.50");
	}

	@Test
	void aDishCannotBeMovedIntoAnotherRestaurantsCategory() {
		MenuItem sezar = item(category(restaurant, "Salatlar", 0), "Sezar");
		MenuCategory foreign = category(otherRestaurant, "Salatlar", 0);

		assertThatThrownBy(() -> service.updateItem(restaurant.getId(), sezar.getId(),
				new MenuItemRequest(foreign.getId(), "Sezar", null, new BigDecimal("8.50")), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void aManagerEditsAndDeletesADish() {
		MenuCategory salads = category(restaurant, "Salatlar", 0);
		MenuCategory mains = category(restaurant, "Əsas yeməklər", 1);
		MenuItem dish = item(salads, "Sezar");

		MenuItemView updated = service.updateItem(restaurant.getId(), dish.getId(),
				new MenuItemRequest(mains.getId(), "Toyuqlu Sezar", "Toyuq, kahı", new BigDecimal("9.90")), manager);
		service.deleteItem(restaurant.getId(), dish.getId(), manager);

		assertThat(updated.categoryId()).isEqualTo(mains.getId());
		assertThat(updated.description()).isEqualTo("Toyuq, kahı");
		assertThat(updated.price()).isEqualByComparingTo("9.90");
		verify(itemRepository).delete(dish);
	}
}
