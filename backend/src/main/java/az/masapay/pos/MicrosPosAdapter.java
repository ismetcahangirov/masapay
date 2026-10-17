package az.masapay.pos;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Oracle Micros integration. Placeholder until a Micros restaurant is onboarded. */
@Component
public class MicrosPosAdapter implements PosAdapter {

	@Override
	public PosType type() {
		return PosType.MICROS;
	}

	@Override
	public PosOrder fetchOrder(Restaurant restaurant, String posOrderId) {
		throw new UnsupportedOperationException("Micros fetchOrder is not implemented yet");
	}

	@Override
	public List<PosOrderLine> syncItems(Restaurant restaurant, String posOrderId) {
		throw new UnsupportedOperationException("Micros syncItems is not implemented yet");
	}

	@Override
	public void closeOrder(Restaurant restaurant, String posOrderId, BigDecimal amountPaid) {
		throw new UnsupportedOperationException("Micros closeOrder is not implemented yet");
	}
}
