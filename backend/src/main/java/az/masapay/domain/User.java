package az.masapay.domain;

import az.masapay.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An application user authenticated through Google. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

	/** Google subject identifier ("sub" claim); stable per Google account. */
	@Column(name = "google_sub", nullable = false, length = 128)
	private String googleSub;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(length = 255)
	private String name;

	@Column(name = "picture_url", length = 1024)
	private String pictureUrl;

	/** Null until an admin provisions the user (EPIC 3, #16). */
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private UserRole role;

	/** Restaurant the staff member belongs to; null for unassigned/super admins. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	/** New Google users start disabled and gain access only once activated. */
	@Column(nullable = false)
	private boolean enabled = false;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;
}
