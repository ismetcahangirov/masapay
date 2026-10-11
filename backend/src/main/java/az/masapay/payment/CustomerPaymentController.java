package az.masapay.payment;

import az.masapay.payment.dto.EqualPaymentQuote;
import az.masapay.payment.dto.EqualPaymentRequest;
import az.masapay.payment.dto.ItemPaymentRequest;
import az.masapay.payment.dto.PaymentQuote;
import az.masapay.payment.dto.PaymentReceipt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public customer payment endpoints, reached via the table QR token: FULL (#24),
 * ITEM (#25) and EQUAL (#26) split modes.
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

	@GetMapping("/items")
	public PaymentQuote quoteItems(@PathVariable UUID qrToken, @RequestParam @NotEmpty List<UUID> itemIds) {
		return paymentSettlementService.quoteItemPayment(qrToken, itemIds);
	}

	@PostMapping("/items")
	public PaymentReceipt payItems(@PathVariable UUID qrToken, @Valid @RequestBody ItemPaymentRequest request) {
		return paymentSettlementService.payItems(qrToken, request.itemIds());
	}

	@GetMapping("/equal")
	public EqualPaymentQuote quoteEqual(@PathVariable UUID qrToken,
			@RequestParam @Min(EqualPaymentRequest.MIN_PARTS) @Max(EqualPaymentRequest.MAX_PARTS) int parts,
			@RequestParam(defaultValue = "1") @Min(1) @Max(EqualPaymentRequest.MAX_PARTS) int partsToPay) {
		return paymentSettlementService.quoteEqualPayment(qrToken, parts, partsToPay);
	}

	@PostMapping("/equal")
	public PaymentReceipt payEqual(@PathVariable UUID qrToken, @Valid @RequestBody EqualPaymentRequest request) {
		return paymentSettlementService.payEqual(qrToken, request.parts(), request.partsToPay());
	}
}
