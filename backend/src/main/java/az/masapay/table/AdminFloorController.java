package az.masapay.table;

import az.masapay.order.dto.TableOrderResponse;
import az.masapay.security.AuthenticatedUser;
import az.masapay.table.dto.FloorTableView;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Live floor plan for the admin panel (#44); later changes arrive on the floor STOMP topic. */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/floor")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER','WAITER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminFloorController {

	private final FloorService floorService;

	public AdminFloorController(FloorService floorService) {
		this.floorService = floorService;
	}

	@GetMapping
	public List<FloorTableView> floor(@PathVariable UUID restaurantId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return floorService.floor(restaurantId, caller);
	}

	/** The full bill of every table that has one, for the staff orders screen (#143). */
	@GetMapping("/bills")
	public List<TableOrderResponse> bills(@PathVariable UUID restaurantId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return floorService.bills(restaurantId, caller);
	}
}
