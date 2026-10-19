package az.masapay.pos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.pos.iiko.IikoClient;
import az.masapay.pos.iiko.IikoOrder;
import az.masapay.pos.iiko.IikoOrderItem;
import az.masapay.pos.iiko.IikoProduct;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class IikoPosAdapterTest {

	private static final String ORDER_ID = "3f2a1c4e-0000-4000-8000-000000000001";
	private static final UUID ORGANIZATION_ID = UUID.fromString("9b1d0000-0000-4000-8000-000000000002");

	private final IikoClient iikoClient = mock(IikoClient.class);
	private final IikoPosAdapter adapter = new IikoPosAdapter(iikoClient);
	private final Restaurant restaurant = new Restaurant();

	@BeforeEach
	void setUp() {
		restaurant.setIikoApiLogin("api-login");
		restaurant.setIikoOrganizationId(ORGANIZATION_ID);
	}

	@Test
	void mapsAnOpenIikoOrderToPosOrder() {
		when(iikoClient.fetchOrder("api-login", ORGANIZATION_ID, UUID.fromString(ORDER_ID)))
				.thenReturn(new IikoOrder("Bill", new BigDecimal("24.50"), List.of()));

		PosOrder order = adapter.fetchOrder(restaurant, ORDER_ID);

		assertThat(order.posOrderId()).isEqualTo(ORDER_ID);
		assertThat(order.totalAmount()).isEqualByComparingTo("24.50");
		assertThat(order.open()).isTrue();
	}

	@Test
	void treatsAClosedIikoOrderAsNotOpen() {
		when(iikoClient.fetchOrder("api-login", ORGANIZATION_ID, UUID.fromString(ORDER_ID)))
				.thenReturn(new IikoOrder("Closed", new BigDecimal("24.50"), List.of()));

		assertThat(adapter.fetchOrder(restaurant, ORDER_ID).open()).isFalse();
	}

	@Test
	void mapsIikoItemsToPosOrderLines() {
		IikoOrderItem plov = new IikoOrderItem(new IikoProduct("p-1", "Plov"), new BigDecimal("2"), new BigDecimal("8.50"));
		when(iikoClient.fetchOrder("api-login", ORGANIZATION_ID, UUID.fromString(ORDER_ID)))
				.thenReturn(new IikoOrder("New", new BigDecimal("17.00"), List.of(plov)));

		List<PosOrderLine> lines = adapter.syncItems(restaurant, ORDER_ID);

		assertThat(lines).containsExactly(
				new PosOrderLine("p-1", "Plov", new BigDecimal("2"), new BigDecimal("8.50")));
	}

	@Test
	void closingBooksThePaymentBeforeClosingTheIikoOrder() {
		UUID paymentTypeId = UUID.fromString("5c2e0000-0000-4000-8000-000000000003");
		restaurant.setIikoPaymentTypeId(paymentTypeId);

		adapter.closeOrder(restaurant, ORDER_ID, new BigDecimal("24.50"));

		InOrder calls = inOrder(iikoClient);
		calls.verify(iikoClient).addPayment("api-login", ORGANIZATION_ID, UUID.fromString(ORDER_ID), paymentTypeId,
				new BigDecimal("24.50"));
		calls.verify(iikoClient).closeOrder("api-login", ORGANIZATION_ID, UUID.fromString(ORDER_ID));
	}
}
