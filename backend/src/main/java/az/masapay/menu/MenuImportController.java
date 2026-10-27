package az.masapay.menu;

import az.masapay.menu.dto.MenuImportDraft;
import az.masapay.menu.dto.MenuImportRequest;
import az.masapay.menu.dto.MenuImportResult;
import az.masapay.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * A whole menu from a PDF or Word file: {@code preview} reads the file and saves nothing, the
 * manager reviews and corrects it, then posts the approved list to {@code import}.
 */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/menu/import")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MenuImportController {

	private final MenuImportService importService;

	public MenuImportController(MenuImportService importService) {
		this.importService = importService;
	}

	@PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public MenuImportDraft preview(@PathVariable UUID restaurantId, @RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal AuthenticatedUser caller) throws IOException {
		return importService.preview(restaurantId, file.getBytes(), caller);
	}

	@PostMapping
	public MenuImportResult apply(@PathVariable UUID restaurantId, @Valid @RequestBody MenuImportRequest request,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return importService.apply(restaurantId, request, caller);
	}
}
