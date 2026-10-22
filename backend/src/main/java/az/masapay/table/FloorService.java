package az.masapay.table;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.order.CustomerOrderService;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.table.dto.FloorTableView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The live floor plan of a restaurant (#44): every table with its floor status, built from the same
 * bill snapshot the table's customers see, so the floor and the table page never disagree.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FloorService {

	private final RestaurantTableRepository tableRepository;
	private final UserRepository userRepository;
	private final CustomerOrderService customerOrderService;

	public FloorService(RestaurantTableRepository tableRepository, UserRepository userRepository,
			CustomerOrderService customerOrderService) {
		this.tableRepository = tableRepository;
		this.userRepository = userRepository;
		this.customerOrderService = customerOrderService;
	}

	@Transactional(readOnly = true)
	public List<FloorTableView> floor(UUID restaurantId, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		Instant now = Instant.now();
		return tableRepository.findByRestaurantIdOrderByLabelAsc(restaurantId).stream()
			.map(table -> FloorTableView.of(customerOrderService.getCurrentOrder(table.getQrToken()), now))
			.toList();
	}

	/**
	 * The bill of every table that has one (#143), in table order: what was ordered, the kitchen
	 * state of each dish, what is paid and what is left. The same snapshot the table's customers
	 * see, so staff and customers never disagree.
	 */
	@Transactional(readOnly = true)
	public List<TableOrderResponse> bills(UUID restaurantId, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		return tableRepository.findByRestaurantIdOrderByLabelAsc(restaurantId).stream()
			.map(table -> customerOrderService.getCurrentOrder(table.getQrToken()))
			.filter(snapshot -> snapshot.order() != null)
			.toList();
	}

	/** Staff may only see their own restaurant's floor; a super admin any. */
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
