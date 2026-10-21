package az.masapay.table;

import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.qr.TableQrUrlBuilder;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.table.dto.TableSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tables of a restaurant for the admin panel. Everyone on the staff (manager and waiters) sees
 * their own restaurant's tables and QR codes; only a manager or super admin creates tables.
 * Each new table gets its own random qrToken, so its QR code is unique.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TableAdminService {

	private final RestaurantTableRepository tableRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final TableQrUrlBuilder urlBuilder;

	public TableAdminService(RestaurantTableRepository tableRepository, RestaurantRepository restaurantRepository,
			UserRepository userRepository, TableQrUrlBuilder urlBuilder) {
		this.tableRepository = tableRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.urlBuilder = urlBuilder;
	}

	@Transactional(readOnly = true)
	public List<TableSummary> list(UUID restaurantId, AuthenticatedUser caller) {
		checkSameRestaurant(caller, restaurantId);
		return tableRepository.findByRestaurantIdOrderByLabelAsc(restaurantId).stream()
			.map(table -> TableSummary.of(table, urlBuilder.urlFor(table)))
			.toList();
	}

	@Transactional
	public TableSummary create(UUID restaurantId, String label, AuthenticatedUser caller) {
		if (caller.role() == UserRole.WAITER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiters cannot create tables");
		}
		checkSameRestaurant(caller, restaurantId);
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
		String name = label.trim();
		boolean taken = tableRepository.findByRestaurantId(restaurantId).stream()
			.anyMatch(table -> table.getLabel().equalsIgnoreCase(name));
		if (taken) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A table with this name already exists");
		}
		RestaurantTable table = new RestaurantTable();
		table.setRestaurant(restaurant);
		table.setLabel(name);
		table = tableRepository.save(table);
		return TableSummary.of(table, urlBuilder.urlFor(table));
	}

	/** Staff may only see their own restaurant's tables; a super admin any. */
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
