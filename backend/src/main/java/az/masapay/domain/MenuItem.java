package az.masapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A dish or drink on the menu (#137). It keeps its own restaurant so ownership checks do not
 * have to go through the category.
 */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
public class MenuItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id", nullable = false, updatable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private MenuCategory category;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;
}
