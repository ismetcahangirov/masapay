package az.masapay.domain;

import az.masapay.domain.enums.KitchenStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single line item on an order. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	/** Reference to the matching item in the external POS, when synced. */
	@Column(name = "pos_item_id", length = 128)
	private String posItemId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal quantity = BigDecimal.ONE;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "total_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalPrice;

	/** True once this item has been settled (item-based split, #25). */
	@Column(nullable = false)
	private boolean paid = false;

	/** The transaction that paid this item, when settled. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "paid_transaction_id")
	private Transaction paidTransaction;

	/** The menu dish a customer ordered (#140); null for lines typed in by staff. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_item_id")
	private MenuItem menuItem;

	/** Kitchen progress (#142); null for lines that never go to the kitchen. */
	@Enumerated(EnumType.STRING)
	@Column(name = "kitchen_status", length = 20)
	private KitchenStatus kitchenStatus;

	/** The staff member who added the line (#144); null for customers and POS syncs. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "added_by")
	private User addedBy;
}
