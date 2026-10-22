package az.masapay.restaurant;

import az.masapay.domain.Restaurant;
import az.masapay.repository.RestaurantTableRepository;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Whether a restaurant is open for business, checked where a customer orders (#140) or starts a
 * payment. A restaurant the super admin switched off takes neither (#147). The answer is 409 with
 * a reason the customer's phone can show.
 */
@Component
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RestaurantGate {

	/** Reason sent with the 409, so the phone can tell it apart from other conflicts. */
	public static final String CLOSED = "RESTAURANT_CLOSED";

	private final RestaurantTableRepository tableRepository;

	public RestaurantGate(RestaurantTableRepository tableRepository) {
		this.tableRepository = tableRepository;
	}

	/** The table's restaurant may take a payment. */
	public void checkTakesPayments(UUID qrToken) {
		checkActive(restaurant(qrToken));
	}

	private Restaurant restaurant(UUID qrToken) {
		return tableRepository.findRestaurantByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
	}

	/** For callers that already hold the restaurant inside a transaction. */
	public static void checkActive(Restaurant restaurant) {
		if (!restaurant.isActive()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, CLOSED);
		}
	}
}
