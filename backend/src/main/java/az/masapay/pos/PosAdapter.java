package az.masapay.pos;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Contract every POS integration implements (#39). Adding a provider means writing one
 * {@code @Component} that implements this interface; {@link PosAdapterRegistry} picks it up.
 */
public interface PosAdapter {

	/** The POS system this adapter talks to. Must be unique across adapters. */
	PosType type();

	/** Reads the bill header from the POS. */
	PosOrder fetchOrder(Restaurant restaurant, String posOrderId);

	/** Reads the current line items of the bill from the POS. */
	List<PosOrderLine> syncItems(Restaurant restaurant, String posOrderId);

	/** Tells the POS the bill was paid through masapay, so it can close the table. */
	void closeOrder(Restaurant restaurant, String posOrderId, BigDecimal amountPaid);
}
