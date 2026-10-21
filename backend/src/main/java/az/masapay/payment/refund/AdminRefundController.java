package az.masapay.payment.refund;

import az.masapay.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin panel refund action (#36): refunds a whole Payriff payment in one call. */
@RestController
@RequestMapping("/api/admin/transactions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminRefundController {

	private final RefundService refundService;

	public AdminRefundController(RefundService refundService) {
		this.refundService = refundService;
	}

	/** Optional body: {@code {"reason": "..."}}. */
	@PostMapping("/{id}/refund")
	public RefundResult refund(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser admin,
			@RequestBody(required = false) RefundRequest request) {
		return refundService.refund(id, admin, request == null ? null : request.reason());
	}

	/** Why the money goes back; shown in Payriff's records. */
	public record RefundRequest(String reason) {}
}
