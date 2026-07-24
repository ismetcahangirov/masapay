package az.masapay.domain;

import az.masapay.domain.enums.TableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A physical restaurant table. Named RestaurantTable to avoid clashing with jakarta.persistence.Table. */
@Entity
@Table(name = "tables")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantTable extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(nullable = false, length = 50)
	private String label;

	/** Opaque token embedded in the table QR code; generated once on creation. */
	@Column(name = "qr_token", nullable = false, updatable = false)
	private UUID qrToken = UUID.randomUUID();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TableStatus status = TableStatus.AVAILABLE;
}
