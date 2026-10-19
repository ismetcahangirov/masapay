package az.masapay.domain;

import az.masapay.domain.enums.PosType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
public class Restaurant extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "pos_type", nullable = false, length = 20)
	private PosType posType = PosType.CUSTOM;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 3)
	private String currency = "AZN";

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "iiko_api_login")
	private String iikoApiLogin;

	@Column(name = "iiko_organization_id")
	private UUID iikoOrganizationId;

	@Column(name = "iiko_payment_type_id")
	private UUID iikoPaymentTypeId;
}
