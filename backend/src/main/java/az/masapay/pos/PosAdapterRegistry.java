package az.masapay.pos;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Picks the {@link PosAdapter} for a restaurant's {@link PosType}. Spring hands in every
 * adapter bean, so a new provider is registered just by adding its {@code @Component}.
 */
@Component
public class PosAdapterRegistry {

	private final Map<PosType, PosAdapter> adapters = new EnumMap<>(PosType.class);

	public PosAdapterRegistry(List<PosAdapter> adapters) {
		for (PosAdapter adapter : adapters) {
			PosAdapter previous = this.adapters.putIfAbsent(adapter.type(), adapter);
			if (previous != null) {
				throw new IllegalStateException("Two POS adapters for " + adapter.type() + ": "
					+ previous.getClass().getSimpleName() + " and " + adapter.getClass().getSimpleName());
			}
		}
	}

	/** The adapter for this POS type; fails if none is registered. */
	public PosAdapter get(PosType type) {
		PosAdapter adapter = adapters.get(type);
		if (adapter == null) {
			throw new IllegalStateException("No POS adapter registered for " + type);
		}
		return adapter;
	}

	/** The adapter for the POS this restaurant syncs with. */
	public PosAdapter forRestaurant(Restaurant restaurant) {
		return get(restaurant.getPosType());
	}
}
