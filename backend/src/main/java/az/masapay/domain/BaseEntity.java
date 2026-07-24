package az.masapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Shared identity and audit columns for every entity: a client-generated random
 * UUID primary key and created/updated timestamps. The database also defaults
 * these columns and maintains updated_at via a trigger, so rows written outside
 * JPA stay consistent.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

	@Id
	@UuidGenerator
	@Column(nullable = false, updatable = false)
	private UUID id;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
