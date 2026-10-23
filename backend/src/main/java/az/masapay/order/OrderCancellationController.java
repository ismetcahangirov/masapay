package az.masapay.order;

import az.masapay.order.dto.CancelOrderRequest;
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

/**
 * Cancelling a table's confirmed order (#186): managers and the super admin only; waiters and
 * chefs get 403 here before the service is even reached.
 */
@RestController
@RequestMapping("/api/admin/tables/{tableId}/order/cancel")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderCancellationController {

	private final OrderCancellationService service;

	public OrderCancellationController(OrderCancellationService service) {
		this.service = service;
	}

	@PostMapping
	public TableOrderResponse cancel(@PathVariable UUID tableId, @Valid @RequestBody CancelOrderRequest request,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return service.cancel(tableId, request.reason(), caller);
	}
}
