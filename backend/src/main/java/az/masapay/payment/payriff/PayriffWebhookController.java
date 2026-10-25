package az.masapay.payment.payriff;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payriff's callback target (#35). Public because Payriff cannot log in; the {@code sig} query
 * parameter we put into the callback URL (#34) is what authenticates the call.
 * <p>
 * The same URL is reached twice per payment: Payriff POSTs the order's state to it, and after
 * 3-D Secure it sends the guest's browser to it with a GET. The GET is answered with a redirect to
 * our result page (found with the Payriff emulator; a plain GET used to end in 405).
 */
@RestController
@RequestMapping("/api/public/payriff")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayriffWebhookController {

	private final PayriffWebhookService webhookService;
	private final PayriffReturnService returnService;

	public PayriffWebhookController(PayriffWebhookService webhookService, PayriffReturnService returnService) {
		this.webhookService = webhookService;
		this.returnService = returnService;
	}

	@PostMapping("/webhook")
	public ResponseEntity<Void> webhook(@RequestParam("tx") String txId, @RequestParam("sig") String sig,
			@RequestBody(required = false) PayriffCallback callback) {
		webhookService.handle(txId, sig, callback);
		return ResponseEntity.ok().build();
	}

	/** The guest's browser, back from the payment page: on to our result page. */
	@GetMapping("/webhook")
	public ResponseEntity<Void> guestReturned(@RequestParam("tx") String txId, @RequestParam("sig") String sig) {
		return ResponseEntity.status(HttpStatus.SEE_OTHER)
			.location(URI.create(returnService.guestReturned(txId, sig)))
			.build();
	}
}
