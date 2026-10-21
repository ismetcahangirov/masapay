package az.masapay.order;

import az.masapay.order.dto.AddOrderItemRequest;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff endpoint to add an item to a table's bill (#21); customers see it live over WebSocket. */
@RestController
@RequestMapping("/api/admin/tables/{tableId}/order")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTO_MANAGER', 'WAITER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaffOrderController {

	private final StaffOrderService staffOrderService;

	public StaffOrderController(StaffOrderService staffOrderService) {
		this.staffOrderService = staffOrderService;
	}

	@PostMapping("/items")
	public TableOrderResponse addItem(@PathVariable UUID tableId, @Valid @RequestBody AddOrderItemRequest request,
			@AuthenticationPrincipal AuthenticatedUser staff) {
		return staffOrderService.addItem(tableId, request, staff);
	}
}
