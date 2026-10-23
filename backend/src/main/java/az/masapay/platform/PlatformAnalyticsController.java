package az.masapay.platform;

import az.masapay.platform.dto.PlatformAnalytics;
import az.masapay.security.AuthenticatedUser;
import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform analytics for the super admin (#149): {@code from} and {@code to} are Baku days
 * (YYYY-MM-DD, both included); without them, the last 30 days up to today.
 */
@RestController
@RequestMapping("/api/admin/platform/analytics")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAnalyticsController {

	private final PlatformAnalyticsService service;

	public PlatformAnalyticsController(PlatformAnalyticsService service) {
		this.service = service;
	}

	@GetMapping
	public PlatformAnalytics analytics(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return service.analytics(from, to, caller);
	}
}
