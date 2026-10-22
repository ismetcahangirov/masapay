package az.masapay.menu;

import az.masapay.menu.dto.MenuCategoryView;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The menu a customer sees after scanning the table QR code: only what can be ordered today
 * (#139). Like the bill, the unguessable QR token is the access capability, so no login.
 */
@RestController
@RequestMapping("/api/public/tables")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerMenuController {

	private final MenuService menuService;

	public CustomerMenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping("/{qrToken}/menu")
	public List<MenuCategoryView> menu(@PathVariable UUID qrToken) {
		return menuService.customerMenu(qrToken);
	}
}
