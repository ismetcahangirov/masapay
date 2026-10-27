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
import az.masapay.domain.MenuItemImage;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.menu.dto.MenuImportCategory;
import az.masapay.menu.dto.MenuImportDraft;
import az.masapay.menu.dto.MenuImportItem;
import az.masapay.menu.dto.MenuImportRequest;
import az.masapay.menu.dto.MenuImportResult;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuComboComponentRepository;
import az.masapay.repository.MenuItemImageRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MenuImportServiceTest {

	private final MenuCategoryRepository categoryRepository = mock(MenuCategoryRepository.class);
	private final MenuItemRepository itemRepository = mock(MenuItemRepository.class);
	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final MenuItemImageRepository imageRepository = mock(MenuItemImageRepository.class);
	private final MenuService menuService = new MenuService(categoryRepository, itemRepository,
		mock(MenuComboComponentRepository.class), restaurantRepository, mock(RestaurantTableRepository.class),
		userRepository);
	private final MenuImportService service = new MenuImportService(menuService,
		new MenuImageService(menuService, imageRepository), categoryRepository, itemRepository, restaurantRepository);

	private final Restaurant restaurant = new Restaurant();
	private final AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);
	private final MenuCategory salads = new MenuCategory();
	private final MenuItem caesar = new MenuItem();
	private final List<MenuItem> saved = new ArrayList<>();

	@BeforeEach
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		User staff = new User();
		staff.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(staff));
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(staff));
		when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
		salads.setId(UUID.randomUUID());
		salads.setRestaurant(restaurant);
		salads.setName("Salatlar");
		salads.setSortOrder(4);
		caesar.setId(UUID.randomUUID());
		caesar.setRestaurant(restaurant);
		caesar.setCategory(salads);
		caesar.setName("Sezar");
		caesar.setPrice(new BigDecimal("8.00"));
		when(categoryRepository.findByRestaurantIdOrderBySortOrderAscNameAsc(restaurant.getId()))
			.thenReturn(List.of(salads));
		when(itemRepository.findByRestaurantIdOrderByNameAsc(restaurant.getId())).thenReturn(List.of(caesar));
		when(categoryRepository.save(any(MenuCategory.class))).thenAnswer(inv -> {
			MenuCategory category = inv.getArgument(0);
			category.setId(UUID.randomUUID());
			return category;
		});
		when(itemRepository.save(any(MenuItem.class))).thenAnswer(inv -> {
			MenuItem item = inv.getArgument(0);
			if (item.getId() == null) {
				item.setId(UUID.randomUUID());
			}
			saved.add(item);
			return item;
		});
		when(imageRepository.findByItemId(any())).thenReturn(Optional.empty());
	}

	private static byte[] docx(String... paragraphs) throws IOException {
		try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			for (String paragraph : paragraphs) {
				document.createParagraph().createRun().setText(paragraph);
			}
			document.write(out);
			return out.toByteArray();
		}
	}

	@Test
	void thePreviewSavesNothingAndMarksWhatIsAlreadyOnTheMenu() throws IOException {
		MenuImportDraft draft = service.preview(restaurant.getId(),
			docx("SALATLAR", "SEZAR 9", "Çoban 6", "İçkilər", "Ayran 2"), manager);

		assertThat(draft.categories()).extracting(MenuImportCategory::name, MenuImportCategory::existing)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("Salatlar", true),
				org.assertj.core.groups.Tuple.tuple("İçkilər", false));
		assertThat(draft.categories().get(0).items()).extracting(MenuImportItem::name, MenuImportItem::existingId)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("SEZAR", caesar.getId()),
				org.assertj.core.groups.Tuple.tuple("Çoban", null));
		verify(itemRepository, never()).save(any());
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void aFileWithNoDishesIsRefused() throws IOException {
		assertThatThrownBy(() -> service.preview(restaurant.getId(), docx("Xoş gəlmisiniz"), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class, e -> {
				assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
				assertThat(e.getReason()).isEqualTo("NO_DISHES");
			});
	}

	@Test
	void approvingUpdatesKnownDishesAndAddsTheRest() throws IOException {
		String photo = "data:image/png;base64," + Base64.getEncoder().encodeToString(MenuImagesTest.png(40, 40));
		MenuImportRequest request = new MenuImportRequest(List.of(
			new MenuImportCategory("salatlar", List.of(
				new MenuImportItem("sezar", "toyuq ilə", new BigDecimal("9.00"), null, null)), true),
			new MenuImportCategory("İçkilər", List.of(
				new MenuImportItem("Ayran", null, new BigDecimal("2.00"), photo, null)), false)));

		MenuImportResult result = service.apply(restaurant.getId(), request, manager);

		assertThat(result).isEqualTo(new MenuImportResult(1, 1, 1));
		assertThat(caesar.getPrice()).isEqualByComparingTo("9");
		assertThat(caesar.getDescription()).isEqualTo("toyuq ilə");
		assertThat(caesar.getCategory()).isSameAs(salads);
		MenuItem ayran = saved.get(1);
		assertThat(ayran.getName()).isEqualTo("Ayran");
		assertThat(ayran.getCategory().getName()).isEqualTo("İçkilər");
		assertThat(ayran.getCategory().getSortOrder()).isEqualTo(5);
		assertThat(ayran.getImageVersion()).isEqualTo(1);
		verify(imageRepository).save(any(MenuItemImage.class));
	}

	@Test
	void theSameDishTwiceIsRefusedBeforeAnythingIsSaved() {
		MenuImportRequest request = new MenuImportRequest(List.of(
			new MenuImportCategory("A", List.of(new MenuImportItem("Çay", null, BigDecimal.ONE, null, null)), false),
			new MenuImportCategory("B", List.of(new MenuImportItem("ÇAY ", null, BigDecimal.TEN, null, null)), false)));

		assertThatThrownBy(() -> service.apply(restaurant.getId(), request, manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getReason()).isEqualTo("DUPLICATE_DISH"));
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void aWaiterCannotImport() {
		assertThatThrownBy(() -> service.apply(restaurant.getId(), new MenuImportRequest(List.of()), waiter))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}
}
