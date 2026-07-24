package az.masapay.payment;

import az.masapay.payment.dto.PaymentQuote;
import az.masapay.payment.dto.PaymentReceipt;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public customer payment endpoints, reached via the table QR token. FULL mode
 * (#24): quote the outstanding amount and pay the whole bill.
 */
@RestController
@RequestMapping("/api/public/tables/{qrToken}/payment")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerPaymentController {

	private final PaymentSettlementService paymentSettlementService;

	public CustomerPaymentController(PaymentSettlementService paymentSettlementService) {
		this.paymentSettlementService = paymentSettlementService;
	}

	@GetMapping("/full")
	public PaymentQuote quoteFull(@PathVariable UUID qrToken) {
		return paymentSettlementService.quoteFullPayment(qrToken);
	}

	@PostMapping("/full")
	public PaymentReceipt payFull(@PathVariable UUID qrToken) {
		return paymentSettlementService.payFull(qrToken);
	}
}
