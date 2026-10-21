package az.masapay.dashboard;

import az.masapay.dashboard.dto.DashboardMetrics;
import az.masapay.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin dashboard KPIs of a restaurant (#43): today's revenue, tips, average table time,
 * Payriff success rate and a daily series of the last {@code days} days (default 7, max 31).
 * A RESTO_MANAGER may only read their own restaurant.
 */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/dashboard")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	public DashboardMetrics metrics(@AuthenticationPrincipal AuthenticatedUser principal,
		@PathVariable UUID restaurantId,
		@RequestParam(defaultValue = "" + DashboardService.DEFAULT_DAYS) int days) {
		return dashboardService.metrics(principal, restaurantId, days);
	}
}
