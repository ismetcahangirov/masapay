package az.masapay.table;

import az.masapay.security.AuthenticatedUser;
import az.masapay.table.dto.CreateTableRequest;
import az.masapay.table.dto.TableSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tables of a restaurant in the admin panel: list for all staff, create for managers (#47). */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/tables")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER','WAITER')")
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminTableController {

	private final TableAdminService tableAdminService;

	public AdminTableController(TableAdminService tableAdminService) {
		this.tableAdminService = tableAdminService;
	}

	@GetMapping
	public List<TableSummary> list(@PathVariable UUID restaurantId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return tableAdminService.list(restaurantId, caller);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
	public TableSummary create(@PathVariable UUID restaurantId, @Valid @RequestBody CreateTableRequest request,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return tableAdminService.create(restaurantId, request.label(), caller);
	}
}
