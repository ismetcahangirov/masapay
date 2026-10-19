package az.masapay.pos;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import az.masapay.pos.iiko.IikoClient;
import az.masapay.pos.iiko.IikoOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** iiko integration: reads the bill through {@link IikoClient} (#40); closing it lands in #41. */
@Component
public class IikoPosAdapter implements PosAdapter {

	private final IikoClient iikoClient;

	public IikoPosAdapter(IikoClient iikoClient) {
		this.iikoClient = iikoClient;
	}

	@Override
	public PosType type() {
		return PosType.IIKO;
	}

	@Override
	public PosOrder fetchOrder(Restaurant restaurant, String posOrderId) {
		IikoOrder order = fetch(restaurant, posOrderId);
		boolean open = !"Closed".equals(order.status()) && !"Deleted".equals(order.status());
		return new PosOrder(posOrderId, order.sum(), open);
	}

	@Override
	public List<PosOrderLine> syncItems(Restaurant restaurant, String posOrderId) {
		return fetch(restaurant, posOrderId).items().stream()
				.map(item -> new PosOrderLine(
						item.product().id(),
						item.product().name(),
						item.amount(),
						item.price()))
				.toList();
	}

	@Override
	public void closeOrder(Restaurant restaurant, String posOrderId, BigDecimal amountPaid) {
		throw new UnsupportedOperationException("iiko closeOrder is not implemented yet (#41)");
	}

	private IikoOrder fetch(Restaurant restaurant, String posOrderId) {
		return iikoClient.fetchOrder(
				restaurant.getIikoApiLogin(),
				restaurant.getIikoOrganizationId(),
				UUID.fromString(posOrderId));
	}
}
