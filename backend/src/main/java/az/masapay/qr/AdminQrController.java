package az.masapay.qr;

import az.masapay.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-panel export of printable table QR codes: one table as SVG, or every table
 * of a restaurant as a ZIP of SVGs. Managers are limited to their own restaurant.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminQrController {

	private static final MediaType SVG = MediaType.parseMediaType("image/svg+xml");
	private static final MediaType ZIP = MediaType.parseMediaType("application/zip");

	private final TableQrExportService exportService;

	public AdminQrController(TableQrExportService exportService) {
		this.exportService = exportService;
	}

	/** Returns the SVG QR code of one table. */
	@GetMapping("/tables/{tableId}/qr")
	public ResponseEntity<String> tableQr(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID tableId) {
		return ResponseEntity.ok()
			.contentType(SVG)
			.header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.inline().filename("table-" + tableId + ".svg").build().toString())
			.body(exportService.tableSvg(caller, tableId));
	}

	/** Downloads a ZIP with one SVG QR code per table of the restaurant. */
	@GetMapping("/restaurants/{restaurantId}/tables/qr")
	public ResponseEntity<byte[]> restaurantQrZip(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID restaurantId) {
		return ResponseEntity.ok()
			.contentType(ZIP)
			.header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.attachment().filename("table-qr-codes-" + restaurantId + ".zip").build().toString())
			.body(exportService.restaurantZip(caller, restaurantId));
	}
}
