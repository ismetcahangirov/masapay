package az.masapay.restaurant;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.SubscriptionStatus;
import az.masapay.repository.RestaurantTableRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Whether a restaurant is open for business, checked where a customer orders (#140) or starts a
 * payment. A restaurant the super admin switched off takes neither (#147); one whose subscription
 * has expired still takes orders but no online payments (#148). The answer is 409 with a reason
 * the customer's phone can show.
 */
@Component
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RestaurantGate {

	/** Restaurants run on Baku time: a subscription ends at the end of its last Baku day. */
	public static final ZoneId ZONE = ZoneId.of("Asia/Baku");

	/** Reasons sent with the 409, so the phone can tell them apart from other conflicts. */
	public static final String CLOSED = "RESTAURANT_CLOSED";
	public static final String SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";

	private final RestaurantTableRepository tableRepository;
	private final Clock clock;

	@Autowired
	public RestaurantGate(RestaurantTableRepository tableRepository) {
		this(tableRepository, Clock.systemUTC());
	}

	public RestaurantGate(RestaurantTableRepository tableRepository, Clock clock) {
		this.tableRepository = tableRepository;
		this.clock = clock;
	}

	/** The table's restaurant may take a payment: it is on and its subscription has not expired. */
	public void checkTakesPayments(UUID qrToken) {
		Restaurant restaurant = restaurant(qrToken);
		checkActive(restaurant);
		if (restaurant.subscriptionStatusOn(LocalDate.now(clock.withZone(ZONE))) == SubscriptionStatus.EXPIRED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, SUBSCRIPTION_EXPIRED);
		}
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
