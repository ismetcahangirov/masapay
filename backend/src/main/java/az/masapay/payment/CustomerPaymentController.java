package az.masapay.payment;

import az.masapay.payment.dto.CheckoutResponse;
import az.masapay.payment.dto.EqualPaymentQuote;
import az.masapay.payment.dto.EqualPaymentRequest;
import az.masapay.payment.dto.FullPaymentRequest;
import az.masapay.payment.dto.ItemPaymentRequest;
import az.masapay.payment.dto.PaymentQuote;
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
 * ITEM (#25) and EQUAL (#26) split modes. Each mode has a quote (GET) and a
 * checkout (POST .../checkout) that starts a Payriff payment and returns the page
 * to send the guest to (#37); the split is applied when Payriff confirms it.
 */
@RestController
@RequestMapping("/api/public/tables/{qrToken}/payment")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerPaymentController {

	private final PaymentSettlementService paymentSettlementService;
	private final PaymentCheckoutService checkoutService;

	public CustomerPaymentController(PaymentSettlementService paymentSettlementService,
			PaymentCheckoutService checkoutService) {
		this.paymentSettlementService = paymentSettlementService;
		this.checkoutService = checkoutService;
	}

	@GetMapping("/full")
	public PaymentQuote quoteFull(@PathVariable UUID qrToken) {
		return paymentSettlementService.quoteFullPayment(qrToken);
	}

	/** The body is optional: without it, or without a tip, the bill is paid without a tip. */
	@PostMapping("/full/checkout")
	public CheckoutResponse checkoutFull(@PathVariable UUID qrToken,
			@Valid @RequestBody(required = false) FullPaymentRequest request) {
		return checkoutService.checkoutFull(qrToken, request == null ? null : request.tipAmount());
	}

	@GetMapping("/items")
	public PaymentQuote quoteItems(@PathVariable UUID qrToken, @RequestParam @NotEmpty List<UUID> itemIds,
			@RequestParam(required = false) @Min(1) @Max(EqualPaymentRequest.MAX_PARTS) Integer guests) {
		return paymentSettlementService.quoteItemPayment(qrToken, itemIds, guests);
	}

	@PostMapping("/items/checkout")
	public CheckoutResponse checkoutItems(@PathVariable UUID qrToken, @Valid @RequestBody ItemPaymentRequest request) {
		return checkoutService.checkoutItems(qrToken, request.itemIds(), request.guests(), request.tipAmount());
	}

	@GetMapping("/equal")
	public EqualPaymentQuote quoteEqual(@PathVariable UUID qrToken,
			@RequestParam @Min(EqualPaymentRequest.MIN_PARTS) @Max(EqualPaymentRequest.MAX_PARTS) int parts,
			@RequestParam(defaultValue = "1") @Min(1) @Max(EqualPaymentRequest.MAX_PARTS) int partsToPay) {
		return paymentSettlementService.quoteEqualPayment(qrToken, parts, partsToPay);
	}

	@PostMapping("/equal/checkout")
	public CheckoutResponse checkoutEqual(@PathVariable UUID qrToken, @Valid @RequestBody EqualPaymentRequest request) {
		return checkoutService.checkoutEqual(qrToken, request.parts(), request.partsToPay(), request.tipAmount());
	}
}
