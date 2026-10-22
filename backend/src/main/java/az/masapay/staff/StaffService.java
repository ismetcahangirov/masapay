package az.masapay.staff;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.staff.dto.InviteStaffRequest;
import az.masapay.staff.dto.StaffMemberView;
import az.masapay.staff.dto.UpdateStaffRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A restaurant manager runs their own waiters and chefs (#145). The manager invites someone by
 * the email of their Google account and picks the role; when that person signs in with Google the
 * account is linked to the invite and waits; the manager then approves it, changes the role or
 * switches it off. A manager only ever sees and changes WAITER and CHEF accounts of their own
 * restaurant: other managers, super admins and other restaurants' staff are out of reach.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaffService {

	/** The roles a manager may give. */
	static final Set<UserRole> STAFF_ROLES = Set.of(UserRole.WAITER, UserRole.CHEF);

	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;

	public StaffService(UserRepository userRepository, RestaurantRepository restaurantRepository) {
		this.userRepository = userRepository;
		this.restaurantRepository = restaurantRepository;
	}

	@Transactional(readOnly = true)
	public List<StaffMemberView> list(UUID restaurantId, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		return userRepository.findByRestaurantIdAndRoleInOrderByNameAscEmailAsc(restaurantId, STAFF_ROLES).stream()
			.map(StaffMemberView::of)
			.toList();
	}

	/**
	 * Invites a waiter or chef. Someone who already signed in but belongs nowhere yet is attached
	 * to this restaurant and waits for approval; anyone already working elsewhere, or a manager or
	 * admin, is refused.
	 */
	@Transactional
	public StaffMemberView invite(UUID restaurantId, InviteStaffRequest request, AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		checkStaffRole(request.role());
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
		String email = request.email().trim().toLowerCase(Locale.ROOT);

		User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
			User invited = new User();
			invited.setEmail(email);
			return invited;
		});
		if (user.getRestaurant() != null || user.getRole() != null) {
			boolean ours = user.getRestaurant() != null && user.getRestaurant().getId().equals(restaurantId);
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				ours ? "Already on this restaurant's staff" : "This account already has another role");
		}
		user.setRestaurant(restaurant);
		user.setRole(request.role());
		user.setEnabled(false);
		user.setDeactivatedAt(null);
		return StaffMemberView.of(userRepository.save(user));
	}

	/** Approves, re-activates or switches off a staff member, or moves them between waiter and chef. */
	@Transactional
	public StaffMemberView update(UUID restaurantId, UUID userId, UpdateStaffRequest request,
			AuthenticatedUser caller) {
		checkManager(caller, restaurantId);
		User user = userRepository.findById(userId)
			.filter(member -> member.getRestaurant() != null && member.getRestaurant().getId().equals(restaurantId))
			.filter(member -> STAFF_ROLES.contains(member.getRole()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
		if (request.role() != null) {
			checkStaffRole(request.role());
			user.setRole(request.role());
		}
		if (Boolean.TRUE.equals(request.active())) {
			if (user.getGoogleSub() == null) {
				// Approve the person, not the email: wait until they have signed in with Google.
				throw new ResponseStatusException(HttpStatus.CONFLICT, "They have not signed in yet");
			}
			user.setEnabled(true);
			user.setDeactivatedAt(null);
		} else if (Boolean.FALSE.equals(request.active())) {
			user.setEnabled(false);
			user.setDeactivatedAt(Instant.now());
		}
		return StaffMemberView.of(userRepository.save(user));
	}

	private static void checkStaffRole(UserRole role) {
		if (!STAFF_ROLES.contains(role)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A manager can give WAITER or CHEF only");
		}
	}

	/** Only the restaurant's own manager, or a super admin. */
	private void checkManager(AuthenticatedUser caller, UUID restaurantId) {
		if (caller.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		if (caller.role() != UserRole.RESTO_MANAGER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Managers only");
		}
		User manager = userRepository.findById(caller.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		if (manager.getRestaurant() == null || !manager.getRestaurant().getId().equals(restaurantId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
	}
}
