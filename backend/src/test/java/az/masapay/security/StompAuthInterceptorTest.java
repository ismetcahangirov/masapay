package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.UserRepository;
import java.security.Principal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class StompAuthInterceptorTest {

	private final JwtService jwtService = new JwtService("test-secret-0123456789-0123456789-abcdef",
			"masapay", Duration.ofMinutes(15), Duration.ofDays(7));
	private final UserRepository userRepository = mock(UserRepository.class);
	@SuppressWarnings("unchecked")
	private final ObjectProvider<UserRepository> users = mock(ObjectProvider.class);
	private final StompAuthInterceptor interceptor = new StompAuthInterceptor(jwtService, users);
	private final MessageChannel channel = mock(MessageChannel.class);

	private final UUID restaurantId = UUID.randomUUID();
	private final String floorTopic = "/topic/restaurants/" + restaurantId + "/floor";

	StompAuthInterceptorTest() {
		when(users.getIfAvailable()).thenReturn(userRepository);
	}

	private User user(UserRole role, UUID restaurant) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "@masapay.az");
		user.setRole(role);
		if (restaurant != null) {
			Restaurant r = new Restaurant();
			r.setId(restaurant);
			user.setRestaurant(r);
		}
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		return user;
	}

	/** Runs a CONNECT frame (optionally with a token) and returns the session principal it set. */
	private Principal connect(String bearerToken) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (bearerToken != null) {
			accessor.addNativeHeader("Authorization", "Bearer " + bearerToken);
		}
		accessor.setLeaveMutable(true);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		interceptor.preSend(message, channel);
		return accessor.getUser();
	}

	@Test
	void aCustomerMayFollowTheirTableWithoutSigningIn() {
		assertThatCode(() -> interceptor.authorize("/topic/tables/" + UUID.randomUUID(), connect(null)))
				.doesNotThrowAnyException();
	}

	@Test
	void anAnonymousClientCannotWatchARestaurantFloor() {
		assertThatThrownBy(() -> interceptor.authorize(floorTopic, connect(null)))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> interceptor.authorize(floorTopic, connect("not-a-jwt")))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void aWaiterWatchesOnlyTheirOwnRestaurant() {
		User waiter = user(UserRole.WAITER, restaurantId);
		Principal session = connect(jwtService.generateAccessToken(waiter));

		assertThat(session).isNotNull();
		assertThatCode(() -> interceptor.authorize(floorTopic, session)).doesNotThrowAnyException();
		assertThatThrownBy(() -> interceptor.authorize("/topic/restaurants/" + UUID.randomUUID() + "/floor", session))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void aSuperAdminMayWatchAnyRestaurant() {
		User admin = user(UserRole.SUPER_ADMIN, null);

		assertThatCode(() -> interceptor.authorize(floorTopic, connect(jwtService.generateAccessToken(admin))))
				.doesNotThrowAnyException();
	}

	@Test
	void aRefreshTokenDoesNotOpenStaffTopics() {
		User admin = user(UserRole.SUPER_ADMIN, null);

		assertThatThrownBy(() -> interceptor.authorize(floorTopic, connect(jwtService.generateRefreshToken(admin))))
				.isInstanceOf(AccessDeniedException.class);
	}
}
