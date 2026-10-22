package az.masapay.order;

import az.masapay.order.dto.PlaceOrderRequest;
import az.masapay.order.dto.TableOrderResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public customer endpoints reached by scanning a table QR code: the live bill, and ordering
 * from the menu (#140). The QR token is an unguessable UUID that acts as the access capability,
 * so no login is required.
 */
@RestController
@RequestMapping("/api/public/tables")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerOrderController {

	private final CustomerOrderService customerOrderService;
	private final CustomerOrderingService orderingService;

	public CustomerOrderController(CustomerOrderService customerOrderService,
			CustomerOrderingService orderingService) {
		this.customerOrderService = customerOrderService;
		this.orderingService = orderingService;
	}

	@GetMapping("/{qrToken}/order")
	public TableOrderResponse currentOrder(@PathVariable UUID qrToken) {
		return customerOrderService.getCurrentOrder(qrToken);
	}

	/** Puts the customer's confirmed basket onto the table's bill (#140). */
	@PostMapping("/{qrToken}/order/items")
	public TableOrderResponse placeOrder(@PathVariable UUID qrToken, @Valid @RequestBody PlaceOrderRequest request) {
		return orderingService.placeOrder(qrToken, request);
	}
}
