package az.masapay.order;

import az.masapay.order.dto.AddMenuItemRequest;
import az.masapay.order.dto.AddOrderItemRequest;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.order.dto.TablePaymentView;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff endpoints for a table's bill: add a typed line (#21) or a menu dish, and see its payments
 * (#144). Customers see every change live over WebSocket.
 */
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

	/** Adds a dish from today's menu at its menu price; it goes to the kitchen (#144). */
	@PostMapping("/menu-items")
	public TableOrderResponse addMenuItem(@PathVariable UUID tableId, @Valid @RequestBody AddMenuItemRequest request,
			@AuthenticationPrincipal AuthenticatedUser staff) {
		return staffOrderService.addMenuItem(tableId, request, staff);
	}

	/** A waiter takes the table: they serve it and get its tips (#182). */
	@PostMapping("/claim")
	public TableOrderResponse claim(@PathVariable UUID tableId, @AuthenticationPrincipal AuthenticatedUser staff) {
		return staffOrderService.claim(tableId, staff);
	}

	/** Payments on the table's current bill, newest first (#144). */
	@GetMapping("/payments")
	public List<TablePaymentView> payments(@PathVariable UUID tableId,
			@AuthenticationPrincipal AuthenticatedUser staff) {
		return staffOrderService.payments(tableId, staff);
	}
}
