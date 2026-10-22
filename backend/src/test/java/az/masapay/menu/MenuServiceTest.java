package az.masapay.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.MenuCategory;
import az.masapay.domain.MenuComboComponent;
import az.masapay.domain.MenuItem;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.menu.dto.ComboComponentRequest;
import az.masapay.menu.dto.ComboComponentView;
import az.masapay.menu.dto.MenuCategoryRequest;
import az.masapay.menu.dto.MenuCategoryView;
import az.masapay.menu.dto.MenuItemRequest;
import az.masapay.menu.dto.MenuItemView;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuComboComponentRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MenuServiceTest {

	private final MenuCategoryRepository categoryRepository = mock(MenuCategoryRepository.class);
	private final MenuItemRepository itemRepository = mock(MenuItemRepository.class);
	private final MenuComboComponentRepository componentRepository = mock(MenuComboComponentRepository.class);
	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
	/** 01:30 on the 22nd in Baku is still the 21st in UTC, so a UTC "today" would be a day early. */
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
	private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T21:30:00Z"), ZoneOffset.UTC);
	private final MenuService service = new MenuService(categoryRepository, itemRepository, componentRepository,
			restaurantRepository, tableRepository, userRepository, clock);

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

	// Combos and business lunches (#138)

	private MenuItemRequest combo(MenuCategory category, String name, String price,
			ComboComponentRequest... components) {
		return new MenuItemRequest(category.getId(), name, null, new BigDecimal(price), List.of(components));
	}

	private void assertStatus(ThrowingCallable call, HttpStatus status) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
			e -> assertThat(e.getStatusCode()).isEqualTo(status));
	}

	@Test
	void aManagerCreatesABusinessLunchFromDishesWithItsOwnPrice() {
		MenuCategory lunch = category(restaurant, "Biznes lanç", 0);
		MenuItem soup = item(category(restaurant, "Şorbalar", 1), "Mərci");
		MenuItem tea = item(category(restaurant, "İçkilər", 2), "Çay");

		MenuItemView created = service.createItem(restaurant.getId(), combo(lunch, "Biznes lanç 1", "12.00",
				new ComboComponentRequest(soup.getId(), 1), new ComboComponentRequest(tea.getId(), 2)), manager);

		assertThat(created.price()).isEqualByComparingTo("12.00");
		assertThat(created.components()).containsExactly(new ComboComponentView(soup.getId(), "Mərci", 1),
				new ComboComponentView(tea.getId(), "Çay", 2));
	}

	@Test
	void editingAComboUpdatesQuantitiesInPlaceAndDropsRemovedDishes() {
		MenuCategory lunch = category(restaurant, "Biznes lanç", 0);
		MenuItem soup = item(category(restaurant, "Şorbalar", 1), "Mərci");
		MenuItem tea = item(category(restaurant, "İçkilər", 2), "Çay");
		MenuItem combo = item(lunch, "Biznes lanç 1");
		combo.replaceComponents(Map.of(soup, 1, tea, 1));
		MenuComboComponent soupRow = combo.getComponents().stream()
			.filter(row -> row.getComponent() == soup)
			.findFirst()
			.orElseThrow();

		MenuItemView updated = service.updateItem(restaurant.getId(), combo.getId(),
				combo(lunch, "Biznes lanç 1", "11.00", new ComboComponentRequest(soup.getId(), 2)), manager);

		assertThat(updated.components()).containsExactly(new ComboComponentView(soup.getId(), "Mərci", 2));
		assertThat(combo.getComponents()).singleElement().isSameAs(soupRow);
	}

	@Test
	void sendingNoComponentsTurnsAComboBackIntoAnOrdinaryDish() {
		MenuCategory lunch = category(restaurant, "Biznes lanç", 0);
		MenuItem soup = item(category(restaurant, "Şorbalar", 1), "Mərci");
		MenuItem combo = item(lunch, "Biznes lanç 1");
		combo.replaceComponents(Map.of(soup, 1));

		MenuItemView updated = service.updateItem(restaurant.getId(), combo.getId(),
				new MenuItemRequest(lunch.getId(), "Biznes lanç 1", null, new BigDecimal("10.00")), manager);

		assertThat(updated.components()).isEmpty();
	}

	@Test
	void aComboCannotContainAnotherComboItselfOrTheSameDishTwice() {
		MenuCategory lunch = category(restaurant, "Biznes lanç", 0);
		MenuItem soup = item(category(restaurant, "Şorbalar", 1), "Mərci");
		MenuItem inner = item(lunch, "Kiçik lanç");
		inner.replaceComponents(Map.of(soup, 1));
		MenuItem outer = item(lunch, "Böyük lanç");

		assertStatus(() -> service.createItem(restaurant.getId(),
				combo(lunch, "X", "1.00", new ComboComponentRequest(inner.getId(), 1)), manager),
			HttpStatus.BAD_REQUEST);
		assertStatus(() -> service.updateItem(restaurant.getId(), outer.getId(),
				combo(lunch, "Böyük lanç", "1.00", new ComboComponentRequest(outer.getId(), 1)), manager),
			HttpStatus.BAD_REQUEST);
		assertStatus(() -> service.createItem(restaurant.getId(), combo(lunch, "X", "1.00",
				new ComboComponentRequest(soup.getId(), 1), new ComboComponentRequest(soup.getId(), 1)), manager),
			HttpStatus.BAD_REQUEST);
		verify(itemRepository, never()).save(any());
	}

	@Test
	void aDishInsideAComboCannotBecomeAComboOrBeDeleted() {
		MenuCategory lunch = category(restaurant, "Biznes lanç", 0);
		MenuItem soup = item(category(restaurant, "Şorbalar", 1), "Mərci");
		MenuItem tea = item(category(restaurant, "İçkilər", 2), "Çay");
		when(componentRepository.existsByComponentId(soup.getId())).thenReturn(true);

		assertStatus(() -> service.updateItem(restaurant.getId(), soup.getId(),
				combo(lunch, "Mərci", "3.00", new ComboComponentRequest(tea.getId(), 1)), manager),
			HttpStatus.BAD_REQUEST);
		assertStatus(() -> service.deleteItem(restaurant.getId(), soup.getId(), manager), HttpStatus.CONFLICT);
		verify(itemRepository, never()).delete(any());
	}

	@Test
	void aComboCannotUseADishOfAnotherRestaurant() {
		MenuCategory lunch = category(restaurant, "Biznes lanç", 0);
		MenuItem foreign = item(category(otherRestaurant, "Şorbalar", 0), "Mərci");

		assertStatus(() -> service.createItem(restaurant.getId(),
				combo(lunch, "X", "1.00", new ComboComponentRequest(foreign.getId(), 1)), manager),
			HttpStatus.NOT_FOUND);
	}
}
