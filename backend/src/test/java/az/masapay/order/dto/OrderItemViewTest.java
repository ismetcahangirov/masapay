package az.masapay.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.domain.MenuItem;
import az.masapay.domain.OrderItem;
import az.masapay.domain.User;
import az.masapay.order.dto.OrderItemView.Source;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderItemViewTest {

	private OrderItem line() {
		OrderItem item = new OrderItem();
		item.setName("Dolma");
		item.setQuantity(BigDecimal.ONE);
		item.setUnitPrice(new BigDecimal("9.00"));
		item.setTotalPrice(new BigDecimal("9.00"));
		item.setCreatedAt(Instant.parse("2026-10-22T12:00:00Z"));
		return item;
	}

	@Test
	void aLineAddedByStaffShowsTheirNameAndTime() {
		OrderItem item = line();
		User waiter = new User();
		waiter.setName("Aysel");
		item.setAddedBy(waiter);
		item.setMenuItem(new MenuItem());

		OrderItemView view = OrderItemView.of(item);

		assertThat(view.source()).isEqualTo(Source.STAFF);
		assertThat(view.addedBy()).isEqualTo("Aysel");
		assertThat(view.addedAt()).isEqualTo(Instant.parse("2026-10-22T12:00:00Z"));
	}

	@Test
	void aMenuDishWithoutStaffCameFromTheCustomer() {
		OrderItem item = line();
		item.setMenuItem(new MenuItem());

		assertThat(OrderItemView.of(item).source()).isEqualTo(Source.CUSTOMER);
		assertThat(OrderItemView.of(item).addedBy()).isNull();
	}

	@Test
	void posLinesAndOlderTypedLinesAreToldApart() {
		OrderItem fromPos = line();
		fromPos.setPosItemId("iiko-42");
		OrderItem olderTyped = line();

		assertThat(OrderItemView.of(fromPos).source()).isEqualTo(Source.POS);
		assertThat(OrderItemView.of(olderTyped).source()).isEqualTo(Source.STAFF);
	}
}
