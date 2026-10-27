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
import az.masapay.menu.dto.MenuItemView;
import az.masapay.repository.MenuCategoryRepository;
import az.masapay.repository.MenuComboComponentRepository;
import az.masapay.repository.MenuItemImageRepository;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MenuImageServiceTest {

	private final MenuItemRepository itemRepository = mock(MenuItemRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final MenuItemImageRepository imageRepository = mock(MenuItemImageRepository.class);
	private final MenuService menuService = new MenuService(mock(MenuCategoryRepository.class), itemRepository,
		mock(MenuComboComponentRepository.class), mock(RestaurantRepository.class),
		mock(RestaurantTableRepository.class), userRepository);
	private final MenuImageService service = new MenuImageService(menuService, imageRepository);

	private final Restaurant restaurant = new Restaurant();
	private final AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
	private final AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@x.az", UserRole.WAITER);
	private final MenuItem dish = new MenuItem();

	@BeforeEach
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		User staff = new User();
		staff.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(staff));
		when(userRepository.findById(waiter.id())).thenReturn(Optional.of(staff));
		MenuCategory category = new MenuCategory();
		category.setId(UUID.randomUUID());
		category.setRestaurant(restaurant);
		dish.setId(UUID.randomUUID());
		dish.setRestaurant(restaurant);
		dish.setCategory(category);
		dish.setName("Plov");
		dish.setPrice(new BigDecimal("12.00"));
		when(itemRepository.findById(dish.getId())).thenReturn(Optional.of(dish));
		when(imageRepository.findByItemId(dish.getId())).thenReturn(Optional.empty());
	}

	@Test
	void aNewPhotoGetsANewVersionedUrl() throws IOException {
		MenuItemView first = service.setImage(restaurant.getId(), dish.getId(), MenuImagesTest.png(50, 50), manager);
		MenuItemView second = service.setImage(restaurant.getId(), dish.getId(), MenuImagesTest.png(60, 60), manager);

		assertThat(first.imageUrl()).isEqualTo("/api/public/menu-images/" + dish.getId() + "?v=1");
		assertThat(second.imageUrl()).endsWith("?v=2");
		verify(imageRepository, org.mockito.Mockito.times(2)).save(any(MenuItemImage.class));
	}

	@Test
	void aFileThatIsNotAPhotoIsRefused() {
		assertThatThrownBy(() -> service.setImage(restaurant.getId(), dish.getId(), "%PDF-1.7".getBytes(), manager))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verify(imageRepository, never()).save(any());
	}

	@Test
	void aWaiterCannotChangeAPhoto() {
		assertThatThrownBy(() -> service.setImage(restaurant.getId(), dish.getId(), MenuImagesTest.png(5, 5), waiter))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void removingThePhotoClearsTheUrl() {
		dish.setImageVersion(3);
		MenuItemImage stored = new MenuItemImage();
		when(imageRepository.findByItemId(dish.getId())).thenReturn(Optional.of(stored));

		MenuItemView view = service.deleteImage(restaurant.getId(), dish.getId(), manager);

		assertThat(view.imageUrl()).isNull();
		verify(imageRepository).delete(stored);
	}
}
