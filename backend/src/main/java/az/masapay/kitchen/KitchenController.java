package az.masapay.kitchen;

import az.masapay.kitchen.dto.KitchenLineView;
import az.masapay.kitchen.dto.KitchenStatusRequest;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The kitchen screen for chefs and managers (#142). */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/kitchen")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER','CHEF')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KitchenController {

	private final KitchenService kitchenService;

	public KitchenController(KitchenService kitchenService) {
		this.kitchenService = kitchenService;
	}

	@GetMapping
	public List<KitchenLineView> work(@PathVariable UUID restaurantId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return kitchenService.work(restaurantId, caller);
	}

	@PutMapping("/items/{itemId}")
	public KitchenLineView mark(@PathVariable UUID restaurantId, @PathVariable UUID itemId,
			@Valid @RequestBody KitchenStatusRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return kitchenService.mark(restaurantId, itemId, request.status(), caller);
	}
}
