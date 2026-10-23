package az.masapay.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.domain.enums.KitchenStatus;
import az.masapay.order.dto.OrderItemView.Source;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TableOrderResponseTest {

	private static final Instant AT = Instant.parse("2026-10-22T12:00:00Z");

	private TableOrderResponse bill(OrderItemView... items) {
		OrderView order = new OrderView(UUID.randomUUID(), "OPEN", new BigDecimal("9.00"), BigDecimal.ZERO,
			new BigDecimal("9.00"), AT, List.of(items), null, null, null);
		return new TableOrderResponse(UUID.randomUUID(), "Masa 4", UUID.randomUUID(), "Dolma", "AZN", null, order);
	}

	@Test
	void customersSeeThatStaffAddedALineButNotWho() {
		OrderItemView byWaiter = new OrderItemView(UUID.randomUUID(), "Dolma", BigDecimal.ONE, new BigDecimal("9.00"),
			new BigDecimal("9.00"), false, KitchenStatus.NEW, Source.STAFF, "Aysel", AT);

		TableOrderResponse staffBill = bill(byWaiter);
		TableOrderResponse forCustomers = staffBill.forCustomers();

		OrderItemView line = forCustomers.order().items().get(0);
		assertThat(line.addedBy()).isNull();
		assertThat(line.source()).isEqualTo(Source.STAFF);
		assertThat(line.addedAt()).isEqualTo(AT);
		assertThat(line).usingRecursiveComparison().ignoringFields("addedBy").isEqualTo(byWaiter);
		assertThat(forCustomers.order()).usingRecursiveComparison().ignoringFields("items")
			.isEqualTo(staffBill.order());
		// The staff copy keeps the name.
		assertThat(staffBill.order().items().get(0).addedBy()).isEqualTo("Aysel");
	}

	@Test
	void aTableWithoutABillIsLeftAsItIs() {
		TableOrderResponse empty = new TableOrderResponse(UUID.randomUUID(), "Masa 1", UUID.randomUUID(), "Dolma",
			"AZN", null, null);

		assertThat(empty.forCustomers()).isSameAs(empty);
	}
}
