package az.masapay.menu;

import az.masapay.menu.dto.MenuCategoryRequest;
import az.masapay.menu.dto.MenuCategoryView;
import az.masapay.menu.dto.MenuItemRequest;
import az.masapay.menu.dto.MenuItemView;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The restaurant menu in the admin panel: read for all staff, changes for managers (#137). */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/menu")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER','WAITER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminMenuController {

	private final MenuService menuService;

	public AdminMenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public List<MenuCategoryView> menu(@PathVariable UUID restaurantId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return menuService.menu(restaurantId, caller);
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public MenuCategoryView createCategory(@PathVariable UUID restaurantId,
			@Valid @RequestBody MenuCategoryRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return menuService.createCategory(restaurantId, request, caller);
	}

	@PutMapping("/categories/{categoryId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public MenuCategoryView updateCategory(@PathVariable UUID restaurantId, @PathVariable UUID categoryId,
			@Valid @RequestBody MenuCategoryRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return menuService.updateCategory(restaurantId, categoryId, request, caller);
	}

	@DeleteMapping("/categories/{categoryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public void deleteCategory(@PathVariable UUID restaurantId, @PathVariable UUID categoryId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		menuService.deleteCategory(restaurantId, categoryId, caller);
	}

	@PostMapping("/items")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public MenuItemView createItem(@PathVariable UUID restaurantId, @Valid @RequestBody MenuItemRequest request,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return menuService.createItem(restaurantId, request, caller);
	}

	@PutMapping("/items/{itemId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public MenuItemView updateItem(@PathVariable UUID restaurantId, @PathVariable UUID itemId,
			@Valid @RequestBody MenuItemRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return menuService.updateItem(restaurantId, itemId, request, caller);
	}

	@DeleteMapping("/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public void deleteItem(@PathVariable UUID restaurantId, @PathVariable UUID itemId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		menuService.deleteItem(restaurantId, itemId, caller);
	}
}
