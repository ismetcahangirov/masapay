package az.masapay.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.staff.dto.InviteStaffRequest;
import az.masapay.staff.dto.StaffMemberView;
import az.masapay.staff.dto.StaffMemberView.Status;
import az.masapay.staff.dto.UpdateStaffRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class StaffServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
	private final StaffService service = new StaffService(userRepository, restaurantRepository);

	private final Restaurant dolma = new Restaurant();
	private final Restaurant other = new Restaurant();
	private final AuthenticatedUser manager =
			new AuthenticatedUser(UUID.randomUUID(), "m@dolma.az", UserRole.RESTO_MANAGER);
	private final AuthenticatedUser waiterCaller =
			new AuthenticatedUser(UUID.randomUUID(), "w@dolma.az", UserRole.WAITER);

	@BeforeEach
	void setUp() {
		dolma.setId(UUID.randomUUID());
		other.setId(UUID.randomUUID());
		User managerUser = new User();
		managerUser.setRestaurant(dolma);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(managerUser));
		when(restaurantRepository.findById(dolma.getId())).thenReturn(Optional.of(dolma));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User user = inv.getArgument(0);
			if (user.getId() == null) {
				user.setId(UUID.randomUUID());
			}
			return user;
		});
	}

	private User member(Restaurant restaurant, UserRole role, boolean signedIn) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(UUID.randomUUID() + "@x.az");
		user.setRestaurant(restaurant);
		user.setRole(role);
		user.setGoogleSub(signedIn ? "sub-" + user.getId() : null);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		return user;
	}

	private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus status) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(status));
	}

	@Test
	void aManagerSeesOnlyTheirWaitersAndChefs() {
		User waiter = member(dolma, UserRole.WAITER, true);
		when(userRepository.findByRestaurantIdAndRoleInOrderByNameAscEmailAsc(dolma.getId(), StaffService.STAFF_ROLES))
				.thenReturn(List.of(waiter));

		assertThat(service.list(dolma.getId(), manager)).extracting(StaffMemberView::id)
				.containsExactly(waiter.getId());
		assertStatus(() -> service.list(other.getId(), manager), HttpStatus.FORBIDDEN);
		assertStatus(() -> service.list(dolma.getId(), waiterCaller), HttpStatus.FORBIDDEN);
	}

	@Test
	void inviteCreatesAWaitingAccountForTheEmail() {
		when(userRepository.findByEmailIgnoreCase("aysel@gmail.com")).thenReturn(Optional.empty());

		StaffMemberView invited = service.invite(dolma.getId(),
				new InviteStaffRequest("  Aysel@Gmail.com ", UserRole.CHEF), manager);

		assertThat(invited.email()).isEqualTo("aysel@gmail.com");
		assertThat(invited.role()).isEqualTo(UserRole.CHEF);
		assertThat(invited.status()).isEqualTo(Status.INVITED);
	}

	@Test
	void someoneWhoSignedInButBelongsNowhereIsAttachedAndWaits() {
		User newcomer = member(null, null, true);
		when(userRepository.findByEmailIgnoreCase(newcomer.getEmail())).thenReturn(Optional.of(newcomer));

		StaffMemberView view = service.invite(dolma.getId(),
				new InviteStaffRequest(newcomer.getEmail(), UserRole.WAITER), manager);

		assertThat(view.status()).isEqualTo(Status.PENDING);
		assertThat(newcomer.getRestaurant()).isSameAs(dolma);
		assertThat(newcomer.isEnabled()).isFalse();
	}

	@Test
	void anAccountWithARoleOrAnotherRestaurantCannotBeInvited() {
		User elsewhere = member(other, UserRole.WAITER, true);
		User admin = member(null, UserRole.SUPER_ADMIN, true);
		when(userRepository.findByEmailIgnoreCase(elsewhere.getEmail())).thenReturn(Optional.of(elsewhere));
		when(userRepository.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));

		assertStatus(() -> service.invite(dolma.getId(), new InviteStaffRequest(elsewhere.getEmail(), UserRole.WAITER),
				manager), HttpStatus.CONFLICT);
		assertStatus(() -> service.invite(dolma.getId(), new InviteStaffRequest(admin.getEmail(), UserRole.WAITER),
				manager), HttpStatus.CONFLICT);
		assertThat(elsewhere.getRestaurant()).isSameAs(other);
		assertThat(admin.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
	}

	@Test
	void aManagerCannotHandOutManagerOrAdminRoles() {
		assertStatus(() -> service.invite(dolma.getId(), new InviteStaffRequest("x@x.az", UserRole.RESTO_MANAGER),
				manager), HttpStatus.BAD_REQUEST);
		User waiter = member(dolma, UserRole.WAITER, true);
		assertStatus(() -> service.update(dolma.getId(), waiter.getId(),
				new UpdateStaffRequest(UserRole.SUPER_ADMIN, null), manager), HttpStatus.BAD_REQUEST);
		assertThat(waiter.getRole()).isEqualTo(UserRole.WAITER);
	}

	@Test
	void approveSwitchRoleAndSwitchOff() {
		User waiter = member(dolma, UserRole.WAITER, true);

		StaffMemberView approved = service.update(dolma.getId(), waiter.getId(),
				new UpdateStaffRequest(UserRole.CHEF, true), manager);
		assertThat(approved.status()).isEqualTo(Status.ACTIVE);
		assertThat(approved.role()).isEqualTo(UserRole.CHEF);

		StaffMemberView off = service.update(dolma.getId(), waiter.getId(), new UpdateStaffRequest(null, false),
				manager);
		assertThat(off.status()).isEqualTo(Status.DEACTIVATED);
		assertThat(waiter.isEnabled()).isFalse();
		assertThat(waiter.getDeactivatedAt()).isNotNull();
	}

	@Test
	void anInviteCannotBeApprovedBeforeTheyHaveSignedIn() {
		User invited = member(dolma, UserRole.WAITER, false);

		assertStatus(() -> service.update(dolma.getId(), invited.getId(), new UpdateStaffRequest(null, true), manager),
				HttpStatus.CONFLICT);
		assertThat(invited.isEnabled()).isFalse();
	}

	@Test
	void otherRestaurantsStaffAndOtherManagersAreOutOfReach() {
		User elsewhere = member(other, UserRole.CHEF, true);
		User colleague = member(dolma, UserRole.RESTO_MANAGER, true);

		assertStatus(() -> service.update(dolma.getId(), elsewhere.getId(), new UpdateStaffRequest(null, false),
				manager), HttpStatus.NOT_FOUND);
		assertStatus(() -> service.update(dolma.getId(), colleague.getId(), new UpdateStaffRequest(null, false),
				manager), HttpStatus.NOT_FOUND);
		verify(userRepository, never()).save(elsewhere);
		verify(userRepository, never()).save(colleague);
	}
}
