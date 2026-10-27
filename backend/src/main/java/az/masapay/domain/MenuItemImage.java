package az.masapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A dish photo. Kept apart from {@link MenuItem} so reading the menu never loads the bytes. */
@Entity
@Table(name = "menu_item_images")
@Getter
@Setter
@NoArgsConstructor
public class MenuItemImage extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id", nullable = false, updatable = false, unique = true)
	private MenuItem item;

	/** image/jpeg, image/png or image/webp. */
	@Column(name = "content_type", nullable = false, length = 30)
	private String contentType;

	@Column(nullable = false)
	private byte[] data;
}
