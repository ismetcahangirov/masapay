package az.masapay.order;

import az.masapay.order.dto.TableOrderResponse;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public customer endpoint reached by scanning a table QR code. The QR token is an
 * unguessable UUID that acts as the access capability, so no login is required.
 */
@RestController
@RequestMapping("/api/public/tables")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerOrderController {

	private final CustomerOrderService customerOrderService;

	public CustomerOrderController(CustomerOrderService customerOrderService) {
		this.customerOrderService = customerOrderService;
	}

	@GetMapping("/{qrToken}/order")
	public TableOrderResponse currentOrder(@PathVariable UUID qrToken) {
		return customerOrderService.getCurrentOrder(qrToken);
	}
}
