package az.masapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One dish inside a combo, e.g. "1 x Mərci şorbası" in "Biznes lanç" (#138). */
@Entity
@Table(name = "menu_combo_components")
@Getter
@Setter
@NoArgsConstructor
public class MenuComboComponent extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "combo_id", nullable = false, updatable = false)
	private MenuItem combo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "component_id", nullable = false)
	private MenuItem component;

	@Column(nullable = false)
	private int quantity = 1;
}
