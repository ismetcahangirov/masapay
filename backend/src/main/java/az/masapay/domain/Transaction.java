package az.masapay.domain;

import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
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

/** A payment against an order, processed through Payriff. */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	/** Bill portion being paid, excluding tip. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "tip_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal tipAmount = BigDecimal.ZERO;

	/** amount + tip_amount, i.e. the charged total. */
	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal refundedAmount = BigDecimal.ZERO;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 3)
	private String currency = "AZN";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransactionStatus status = TransactionStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "split_type", length = 20)
	private SplitType splitType;

	@Column(name = "payment_method", length = 30)
	private String paymentMethod;

	/** Payriff order id; the idempotency handle for webhook processing (#35). */
	@Column(name = "payriff_order_id", length = 128)
	private String payriffOrderId;

	@Column(name = "payriff_payment_id", length = 128)
	private String payriffPaymentId;

	@Column(name = "paid_at")
	private Instant paidAt;
}
