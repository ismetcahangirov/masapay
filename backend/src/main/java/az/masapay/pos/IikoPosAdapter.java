package az.masapay.pos;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** iiko integration. Reading the bill lands in #40, closing it in #41. */
@Component
public class IikoPosAdapter implements PosAdapter {

	@Override
	public PosType type() {
		return PosType.IIKO;
	}

	@Override
	public PosOrder fetchOrder(Restaurant restaurant, String posOrderId) {
		throw new UnsupportedOperationException("iiko fetchOrder is not implemented yet (#40)");
	}

	@Override
	public List<PosOrderLine> syncItems(Restaurant restaurant, String posOrderId) {
		throw new UnsupportedOperationException("iiko syncItems is not implemented yet (#40)");
	}

	@Override
	public void closeOrder(Restaurant restaurant, String posOrderId, BigDecimal amountPaid) {
		throw new UnsupportedOperationException("iiko closeOrder is not implemented yet (#41)");
	}
}
