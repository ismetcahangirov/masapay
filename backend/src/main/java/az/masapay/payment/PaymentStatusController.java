package az.masapay.payment;

import az.masapay.payment.dto.PaymentStatusView;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Status of one payment for the result page Payriff sends the guest back to (#37). Public: the
 * random transaction id from the guest's own checkout is the only key, like the table qrToken.
 */
@RestController
@RequestMapping("/api/public/payments")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentStatusController {

	private final PaymentCheckoutService checkoutService;

	public PaymentStatusController(PaymentCheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@GetMapping("/{transactionId}")
	public PaymentStatusView status(@PathVariable UUID transactionId) {
		return checkoutService.status(transactionId);
	}
}
