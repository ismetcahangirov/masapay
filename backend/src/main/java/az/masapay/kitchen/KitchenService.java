package az.masapay.kitchen;

import az.masapay.domain.OrderItem;
import az.masapay.domain.User;
import az.masapay.domain.enums.KitchenStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.kitchen.dto.KitchenLineView;
import az.masapay.realtime.OrderRealtimeBroadcaster;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The kitchen screen (#142). Dishes customers ordered from the menu arrive as NEW; the chef marks
 * them PREPARING and READY. Every change goes out on the table's live update, which also pings the
 * restaurant's kitchen topic, so all kitchen screens and the table see it without a reload.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KitchenService {

	/** Ready dishes stay on the screen this long, so the chef sees what was just finished. */
	static final Duration READY_SHOWN_FOR = Duration.ofMinutes(30);

	private final OrderItemRepository orderItemRepository;
	private final UserRepository userRepository;
	private final ObjectProvider<OrderRealtimeBroadcaster> broadcaster;
	private final Clock clock;

	@Autowired
	public KitchenService(OrderItemRepository orderItemRepository, UserRepository userRepository,
			ObjectProvider<OrderRealtimeBroadcaster> broadcaster) {
		this(orderItemRepository, userRepository, broadcaster, Clock.systemUTC());
	}

	KitchenService(OrderItemRepository orderItemRepository, UserRepository userRepository,
			ObjectProvider<OrderRealtimeBroadcaster> broadcaster, Clock clock) {
		this.orderItemRepository = orderItemRepository;
		this.userRepository = userRepository;
		this.broadcaster = broadcaster;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<KitchenLineView> work(UUID restaurantId, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		return orderItemRepository.findKitchenWork(restaurantId, List.of(KitchenStatus.NEW, KitchenStatus.PREPARING),
				KitchenStatus.READY, clock.instant().minus(READY_SHOWN_FOR))
			.stream()
			.map(KitchenLineView::of)
			.toList();
	}

	@Transactional
	public KitchenLineView mark(UUID restaurantId, UUID itemId, KitchenStatus status, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		OrderItem item = orderItemRepository.findById(itemId)
			.filter(line -> line.getOrder().getRestaurant().getId().equals(restaurantId))
			.filter(line -> line.getKitchenStatus() != null)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kitchen line not found"));
		item.setKitchenStatus(status);
		orderItemRepository.save(item);
		UUID qrToken = item.getOrder().getTable().getQrToken();
		broadcaster.ifAvailable(b -> b.broadcastOrderUpdate(qrToken));
		return KitchenLineView.of(item);
	}

	/** Kitchen staff and managers see only their own restaurant's kitchen; a super admin any. */
	private void checkSameRestaurant(AuthenticatedUser caller, UUID restaurantId) {
		if (caller.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		User user = userRepository.findById(caller.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(restaurantId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
	}
}
