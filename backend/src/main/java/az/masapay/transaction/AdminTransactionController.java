package az.masapay.transaction;

import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.security.AuthenticatedUser;
import az.masapay.transaction.dto.TransactionPage;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transactions table of the admin panel (#45): filter by status, split type and Baku date range,
 * search by Payriff order id, transaction id prefix or table label; newest first, paged.
 */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/transactions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminTransactionController {

	private final TransactionSearchService transactionSearchService;

	public AdminTransactionController(TransactionSearchService transactionSearchService) {
		this.transactionSearchService = transactionSearchService;
	}

	@GetMapping
	public TransactionPage search(@AuthenticationPrincipal AuthenticatedUser principal,
		@PathVariable UUID restaurantId,
		@RequestParam(required = false) TransactionStatus status,
		@RequestParam(required = false) SplitType splitType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		@RequestParam(required = false) String q,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return transactionSearchService.search(principal, restaurantId, status, splitType, from, to, q, page,
			size);
	}

	/** Everything the same filters match, as a formatted Excel workbook (#170). */
	@GetMapping("/export/xlsx")
	public ResponseEntity<byte[]> exportExcel(@AuthenticationPrincipal AuthenticatedUser principal,
		@PathVariable UUID restaurantId,
		@RequestParam(required = false) TransactionStatus status,
		@RequestParam(required = false) SplitType splitType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		@RequestParam(required = false) String q) {
		return transactionSearchService.excel(principal, restaurantId, status, splitType, from, to, q)
			.asXlsxDownload();
	}
}
