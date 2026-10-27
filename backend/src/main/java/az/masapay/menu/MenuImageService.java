package az.masapay.menu;

import az.masapay.domain.MenuItem;
import az.masapay.domain.MenuItemImage;
import az.masapay.menu.dto.MenuItemView;
import az.masapay.repository.MenuItemImageRepository;
import az.masapay.security.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dish photos: a manager sets or removes one, anyone reads it. Only the manager's own restaurant,
 * like the rest of the menu ({@link MenuService} does the checks).
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MenuImageService {

	private final MenuService menuService;
	private final MenuItemImageRepository imageRepository;

	public MenuImageService(MenuService menuService, MenuItemImageRepository imageRepository) {
		this.menuService = menuService;
		this.imageRepository = imageRepository;
	}

	/** Stores the photo, shrunk; a file that is not a JPEG, PNG or WebP photo answers 400. */
	@Transactional
	public MenuItemView setImage(UUID restaurantId, UUID itemId, byte[] file, AuthenticatedUser caller) {
		menuService.checkManager(caller, restaurantId);
		MenuItem item = menuService.item(restaurantId, itemId);
		MenuImages.Image image = MenuImages.normalise(file);
		if (image == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE");
		}
		store(item, image);
		return MenuItemView.of(item, menuService.today());
	}

	@Transactional
	public MenuItemView deleteImage(UUID restaurantId, UUID itemId, AuthenticatedUser caller) {
		menuService.checkManager(caller, restaurantId);
		MenuItem item = menuService.item(restaurantId, itemId);
		imageRepository.findByItemId(itemId).ifPresent(imageRepository::delete);
		item.setImageVersion(0);
		return MenuItemView.of(item, menuService.today());
	}

	/** Sets the photo of a saved dish the caller may change; also used by the menu import. */
	void store(MenuItem item, MenuImages.Image image) {
		MenuItemImage row = imageRepository.findByItemId(item.getId()).orElseGet(() -> {
			MenuItemImage created = new MenuItemImage();
			created.setItem(item);
			return created;
		});
		row.setContentType(image.contentType());
		row.setData(image.data());
		imageRepository.save(row);
		item.setImageVersion(item.getImageVersion() + 1);
	}

	@Transactional(readOnly = true)
	public Optional<MenuItemImage> image(UUID itemId) {
		return imageRepository.findByItemId(itemId);
	}
}
