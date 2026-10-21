package az.masapay.admin;

import az.masapay.admin.dto.IntegrationSettings;
import az.masapay.admin.dto.UpdateIntegrationSettingsRequest;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
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

/**
 * POS and Payriff integration settings of a restaurant (admin panel form).
 * Secrets are only ever returned masked.
 */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/integration-settings")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationSettingsController {

	private final IntegrationSettingsService integrationSettingsService;

	public IntegrationSettingsController(IntegrationSettingsService integrationSettingsService) {
		this.integrationSettingsService = integrationSettingsService;
	}

	@GetMapping
	public IntegrationSettings get(
			@PathVariable UUID restaurantId, @AuthenticationPrincipal AuthenticatedUser principal) {
		return integrationSettingsService.getSettings(restaurantId, principal);
	}

	@PutMapping
	public IntegrationSettings update(
			@PathVariable UUID restaurantId,
			@Valid @RequestBody UpdateIntegrationSettingsRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return integrationSettingsService.updateSettings(restaurantId, request, principal);
	}
}
