package az.masapay.staff;

import az.masapay.security.AuthenticatedUser;
import az.masapay.staff.dto.InviteStaffRequest;
import az.masapay.staff.dto.StaffMemberView;
import az.masapay.staff.dto.UpdateStaffRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A restaurant's waiters and chefs, run by its manager (#145). */
@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}/staff")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','RESTO_MANAGER')")
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaffController {

	private final StaffService staffService;

	public StaffController(StaffService staffService) {
		this.staffService = staffService;
	}

	@GetMapping
	public List<StaffMemberView> list(@PathVariable UUID restaurantId,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return staffService.list(restaurantId, caller);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public StaffMemberView invite(@PathVariable UUID restaurantId, @Valid @RequestBody InviteStaffRequest request,
			@AuthenticationPrincipal AuthenticatedUser caller) {
		return staffService.invite(restaurantId, request, caller);
	}

	@PatchMapping("/{userId}")
	public StaffMemberView update(@PathVariable UUID restaurantId, @PathVariable UUID userId,
			@RequestBody UpdateStaffRequest request, @AuthenticationPrincipal AuthenticatedUser caller) {
		return staffService.update(restaurantId, userId, request, caller);
	}
}
