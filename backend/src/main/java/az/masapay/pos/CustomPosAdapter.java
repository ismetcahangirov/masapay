package az.masapay.pos;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** A restaurant's own in-house POS, reached over its custom API. Placeholder for now. */
@Component
public class CustomPosAdapter implements PosAdapter {

	@Override
	public PosType type() {
		return PosType.CUSTOM;
	}

	@Override
	public PosOrder fetchOrder(Restaurant restaurant, String posOrderId) {
		throw new UnsupportedOperationException("Custom POS fetchOrder is not implemented yet");
	}

	@Override
	public List<PosOrderLine> syncItems(Restaurant restaurant, String posOrderId) {
		throw new UnsupportedOperationException("Custom POS syncItems is not implemented yet");
	}

	@Override
	public void closeOrder(Restaurant restaurant, String posOrderId, BigDecimal amountPaid) {
		throw new UnsupportedOperationException("Custom POS closeOrder is not implemented yet");
	}
}
