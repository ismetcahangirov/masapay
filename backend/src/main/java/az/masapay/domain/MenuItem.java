package az.masapay.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.BatchSize;
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

	/** False while the manager has switched the dish off (#139). */
	@Column(nullable = false)
	private boolean active = true;

	/** The one Asia/Baku day the dish is off, e.g. today because the chef decided so. */
	@Column(name = "off_on")
	private LocalDate offOn;

	/** First and last day the dish is served, both included; null is open-ended. */
	@Column(name = "available_from")
	private LocalDate availableFrom;

	@Column(name = "available_until")
	private LocalDate availableUntil;

	/** The dishes a combo is made of (#138); empty for an ordinary dish. */
	@OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchSize(size = 50)
	private List<MenuComboComponent> components = new ArrayList<>();

	public boolean isCombo() {
		return !components.isEmpty();
	}

	/**
	 * Whether customers can see and order the dish on {@code day}. A combo also needs every dish
	 * inside it: a business lunch without its soup cannot be served.
	 */
	public boolean isAvailableOn(LocalDate day) {
		boolean own = active
			&& !day.equals(offOn)
			&& (availableFrom == null || !day.isBefore(availableFrom))
			&& (availableUntil == null || !day.isAfter(availableUntil));
		return own && components.stream().allMatch(row -> row.getComponent().isAvailableOn(day));
	}

	/**
	 * Makes the combo contain exactly these dishes and quantities. Existing rows are updated in
	 * place rather than cleared and re-added: Hibernate flushes inserts before deletes, so
	 * re-adding the same dish would break the (combo, component) unique key.
	 */
	public void replaceComponents(Map<MenuItem, Integer> wanted) {
		Set<UUID> wantedIds = wanted.keySet().stream().map(MenuItem::getId).collect(Collectors.toSet());
		components.removeIf(row -> !wantedIds.contains(row.getComponent().getId()));
		wanted.forEach((dish, quantity) -> components.stream()
			.filter(row -> row.getComponent().getId().equals(dish.getId()))
			.findFirst()
			.ifPresentOrElse(row -> row.setQuantity(quantity), () -> {
				MenuComboComponent row = new MenuComboComponent();
				row.setCombo(this);
				row.setComponent(dish);
				row.setQuantity(quantity);
				components.add(row);
			}));
	}
}
