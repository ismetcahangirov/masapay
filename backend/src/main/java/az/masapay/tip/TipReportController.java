package az.masapay.tip;

import az.masapay.security.AuthenticatedUser;
import az.masapay.tip.dto.TipReport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-waiter tip report of a restaurant (#48), as JSON or as a CSV export.
 * {@code from}/{@code to} are inclusive
 * ISO dates (Asia/Baku); both are optional and default to the current month so far.
 * A RESTO_MANAGER may only read their own restaurant.
 */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/tips")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TipReportController {

	private final TipReportService tipReportService;

	public TipReportController(TipReportService tipReportService) {
		this.tipReportService = tipReportService;
	}

	@GetMapping
	public TipReport report(@AuthenticationPrincipal AuthenticatedUser principal,
		@PathVariable UUID restaurantId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return tipReportService.report(principal, restaurantId, from, to);
	}

	/** The same report as a UTF-8 CSV download. */
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthenticatedUser principal,
		@PathVariable UUID restaurantId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		TipReport report = tipReportService.report(principal, restaurantId, from, to);
		String filename = "tips-" + report.from() + "-" + report.to() + ".csv";
		return ResponseEntity.ok()
			.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
			.header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.attachment().filename(filename).build().toString())
			.body(TipReportCsvWriter.write(report).getBytes(StandardCharsets.UTF_8));
	}
}
