package az.masapay.platform;

import az.masapay.platform.dto.AddManagerRequest;
import az.masapay.platform.dto.CreateRestaurantRequest;
import az.masapay.platform.dto.PlatformRestaurantView;
import az.masapay.platform.dto.SubscriptionRequest;
import az.masapay.platform.dto.UpdateRestaurantRequest;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The platform's restaurants and their subscriptions, run by the super admin (#147, #148). */
@RestController
@RequestMapping("/api/admin/platform/restaurants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformRestaurantController {

	private final PlatformRestaurantService service;

	public PlatformRestaurantController(PlatformRestaurantService service) {
		this.service = service;
	}

	@GetMapping
	public List<PlatformRestaurantView> list(@AuthenticationPrincipal AuthenticatedUser caller) {
		return service.list(caller);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlatformRestaurantView create(@Valid @RequestBody CreateRestaurantRequest request,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return service.create(request, caller);
	}

	@PatchMapping("/{restaurantId}")
	public PlatformRestaurantView update(@PathVariable UUID restaurantId,
			@Valid @RequestBody UpdateRestaurantRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return service.update(restaurantId, request, caller);
	}

	@PutMapping("/{restaurantId}/subscription")
	public PlatformRestaurantView setSubscription(@PathVariable UUID restaurantId,
			@Valid @RequestBody SubscriptionRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return service.setSubscription(restaurantId, request, caller);
	}

	@PostMapping("/{restaurantId}/managers")
	public PlatformRestaurantView addManager(@PathVariable UUID restaurantId,
			@Valid @RequestBody AddManagerRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return service.addManager(restaurantId, request.email(), caller);
	}
}
