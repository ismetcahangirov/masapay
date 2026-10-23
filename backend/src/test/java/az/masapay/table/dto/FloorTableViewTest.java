package az.masapay.table.dto;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.table.FloorStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FloorTableViewTest {

	private static final Instant OPENED = Instant.parse("2026-10-21T12:00:00Z");
	private static final Instant NOW = Instant.parse("2026-10-21T12:25:30Z");

	private static TableOrderResponse snapshot(OrderView order) {
		return new TableOrderResponse(UUID.randomUUID(), "Masa 3", UUID.randomUUID(), "Cafe", "AZN", null, order);
	}

	private static OrderView order(String status, String total, String paid) {
		BigDecimal totalAmount = new BigDecimal(total);
		BigDecimal paidAmount = new BigDecimal(paid);
		return new OrderView(UUID.randomUUID(), status, totalAmount, paidAmount,
			totalAmount.subtract(paidAmount).max(BigDecimal.ZERO), OPENED, List.of(), null, null, null);
	}

	@Test
	void noBillIsAnEmptyTable() {
		FloorTableView view = FloorTableView.of(snapshot(null), NOW);

		assertThat(view.status()).isEqualTo(FloorStatus.EMPTY);
		assertThat(view.label()).isEqualTo("Masa 3");
		assertThat(view.currency()).isEqualTo("AZN");
		assertThat(view.totalAmount()).isNull();
		assertThat(view.minutesOpen()).isNull();
	}

	@Test
	void anOpenBillWithNothingPaidIsActiveWithItsOpenMinutes() {
		FloorTableView view = FloorTableView.of(snapshot(order("OPEN", "40.00", "0")), NOW);

		assertThat(view.status()).isEqualTo(FloorStatus.ACTIVE);
		assertThat(view.outstanding()).isEqualByComparingTo("40.00");
		assertThat(view.minutesOpen()).isEqualTo(25L);
	}

	@Test
	void anOpenBillWithAPaymentIsPartiallyPaid() {
		FloorTableView view = FloorTableView.of(snapshot(order("OPEN", "40.00", "15.00")), NOW);

		assertThat(view.status()).isEqualTo(FloorStatus.PARTIALLY_PAID);
		assertThat(view.paidAmount()).isEqualByComparingTo("15.00");
		assertThat(view.outstanding()).isEqualByComparingTo("25.00");
	}

	@Test
	void aPaidBillInTheReceiptWindowIsClosed() {
		FloorTableView view = FloorTableView.of(snapshot(order("PAID", "40.00", "40.00")), NOW);

		assertThat(view.status()).isEqualTo(FloorStatus.CLOSED);
		assertThat(view.minutesOpen()).isNull();
	}
}
