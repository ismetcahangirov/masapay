package az.masapay.menu;

import az.masapay.menu.dto.MenuItemView;
import az.masapay.security.AuthenticatedUser;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dish photos. Managers set and remove them; the photo itself is public, like the customer menu,
 * and its URL carries a version, so browsers and proxies may keep it for a year.
 */
@RestController
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MenuImageController {

	private final MenuImageService imageService;

	public MenuImageController(MenuImageService imageService) {
		this.imageService = imageService;
	}

	@PutMapping(path = "/api/admin/restaurants/{restaurantId}/menu/items/{itemId}/image",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public MenuItemView setImage(@PathVariable UUID restaurantId, @PathVariable UUID itemId,
			@RequestParam("file") MultipartFile file, @AuthenticationPrincipal AuthenticatedUser caller)
			throws IOException {
		return imageService.setImage(restaurantId, itemId, file.getBytes(), caller);
	}

	@DeleteMapping("/api/admin/restaurants/{restaurantId}/menu/items/{itemId}/image")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public MenuItemView deleteImage(@PathVariable UUID restaurantId, @PathVariable UUID itemId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return imageService.deleteImage(restaurantId, itemId, caller);
	}

	@GetMapping("/api/public/menu-images/{itemId}")
	public ResponseEntity<byte[]> image(@PathVariable UUID itemId) {
		return imageService.image(itemId)
			.map(image -> ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.getContentType()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
				.header("X-Content-Type-Options", "nosniff")
				.body(image.getData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
