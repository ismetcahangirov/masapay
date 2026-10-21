package az.masapay.domain;

import az.masapay.domain.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An open or settled bill (adisyon) for a table. */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "table_id", nullable = false)
	private RestaurantTable table;

	/** Waiter serving the table; null when unknown (e.g. not sent by the POS). Drives the tip report (#48). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "waiter_id")
	private User waiter;

	/** Reference to the matching order in the external POS, when synced. */
	@Column(name = "pos_order_id", length = 128)
	private String posOrderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.OPEN;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 3)
	private String currency = "AZN";

	@Column(name = "opened_at", nullable = false)
	private Instant openedAt = Instant.now();

	@Column(name = "closed_at")
	private Instant closedAt;

	/** Number of parts the bill is split into for EQUAL mode (#26); null until an equal payer fixes it. */
	@Column(name = "equal_split_parts")
	private Integer equalSplitParts;

	/** The amount divided by the equal split, fixed when the split starts. */
	@Column(name = "equal_split_base", precision = 12, scale = 2)
	private BigDecimal equalSplitBase;

	/** How many of the equal parts have been paid so far. */
	@Column(name = "equal_split_paid_parts", nullable = false)
	private int equalSplitPaidParts;

	/** Guests sharing the service charge in ITEM mode (#25); null until the first item payer fixes it. */
	@Column(name = "service_charge_guests")
	private Integer serviceChargeGuests;

	/** How many guests' service-charge shares have been paid so far. */
	@Column(name = "service_charge_paid_shares", nullable = false)
	private int serviceChargePaidShares;
}
