package az.masapay.payment.payriff;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payriff's callback target (#35). Public because Payriff cannot log in; the {@code sig} query
 * parameter we put into the callback URL (#34) is what authenticates the call.
 */
@RestController
@RequestMapping("/api/public/payriff")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffWebhookController {

	private final PayriffWebhookService webhookService;

	public PayriffWebhookController(PayriffWebhookService webhookService) {
		this.webhookService = webhookService;
	}

	@PostMapping("/webhook")
	public ResponseEntity<Void> webhook(@RequestParam("tx") String txId, @RequestParam("sig") String sig,
			@RequestBody(required = false) PayriffCallback callback) {
		webhookService.handle(txId, sig, callback);
		return ResponseEntity.ok().build();
	}
}
