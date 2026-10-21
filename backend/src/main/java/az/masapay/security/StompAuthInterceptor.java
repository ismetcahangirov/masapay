package az.masapay.security;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Guards the staff-only STOMP topics. Customer topics ({@code /topic/tables/{qrToken}}) stay open:
 * the random qrToken is the capability, as on the REST side. The restaurant topics
 * ({@code /topic/restaurants/{restaurantId}/...}, e.g. the live floor of #44) carry every table's
 * totals, so subscribing needs a valid access token sent as the {@code Authorization: Bearer ...}
 * header of the STOMP CONNECT frame, and the user must work at that restaurant (a super admin may
 * watch any). A refused subscription ends the connection with a STOMP ERROR frame.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

	static final String RESTAURANT_TOPICS = "/topic/restaurants/";
	private static final String BEARER = "Bearer ";

	private final JwtService jwtService;
	private final ObjectProvider<UserRepository> userRepository;

	public StompAuthInterceptor(JwtService jwtService, ObjectProvider<UserRepository> userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		if (accessor.getCommand() == StompCommand.CONNECT) {
			authenticate(accessor);
		} else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
			authorize(accessor.getDestination(), accessor.getUser());
		}
		return message;
	}

	/** Attaches the user of a valid access token to the session; without one it stays anonymous. */
	private void authenticate(StompHeaderAccessor accessor) {
		String header = accessor.getFirstNativeHeader("Authorization");
		if (header == null || !header.startsWith(BEARER)) {
			return;
		}
		try {
			Claims claims = jwtService.parse(header.substring(BEARER.length()).trim());
			if (!JwtService.TYPE_ACCESS.equals(jwtService.getType(claims))) {
				return;
			}
			UserRole role = jwtService.getRole(claims);
			AuthenticatedUser principal =
				new AuthenticatedUser(jwtService.getUserId(claims), jwtService.getEmail(claims), role);
			List<SimpleGrantedAuthority> authorities =
				role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
			accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, authorities));
		} catch (JwtException | IllegalArgumentException ex) {
			// Invalid or expired token: stay anonymous; restaurant topics will then be refused.
		}
	}

	/** Restaurant topics are for that restaurant's staff only. */
	void authorize(String destination, Principal user) {
		if (destination == null || !destination.startsWith(RESTAURANT_TOPICS)) {
			return;
		}
		UUID restaurantId = restaurantIdOf(destination);
		AuthenticatedUser caller = user instanceof UsernamePasswordAuthenticationToken token
				&& token.getPrincipal() instanceof AuthenticatedUser authenticated ? authenticated : null;
		if (caller == null || caller.role() == null) {
			throw new AccessDeniedException("Sign in to watch this restaurant");
		}
		if (caller.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		UserRepository users = userRepository.getIfAvailable();
		UUID staffRestaurant = users == null ? null : users.findById(caller.id())
			.map(User::getRestaurant)
			.map(Restaurant::getId)
			.orElse(null);
		if (!Objects.equals(staffRestaurant, restaurantId)) {
			throw new AccessDeniedException("Not your restaurant");
		}
	}

	private static UUID restaurantIdOf(String destination) {
		String rest = destination.substring(RESTAURANT_TOPICS.length());
		int slash = rest.indexOf('/');
		try {
			return UUID.fromString(slash < 0 ? rest : rest.substring(0, slash));
		} catch (IllegalArgumentException ex) {
			throw new AccessDeniedException("Unknown restaurant topic");
		}
	}
}
