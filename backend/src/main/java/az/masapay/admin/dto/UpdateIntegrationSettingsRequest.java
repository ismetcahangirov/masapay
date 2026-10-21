package az.masapay.admin.dto;

import az.masapay.domain.enums.PosType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Partial update of a restaurant's integration settings. Null fields are left
 * unchanged. For the secrets (iikoApiLogin, payriffSecretKey) a blank value or
 * the masked placeholder returned by GET also means "keep the stored secret",
 * so the form can send back what it received. A blank payriffMerchantId clears it.
 * Service charge: {@code serviceChargeEnabled = false} switches it off; {@code true} sets
 * {@code serviceChargePercent} (required then).
 */
public record UpdateIntegrationSettingsRequest(
		PosType posType,
		@Size(max = 255) String iikoApiLogin,
		UUID iikoOrganizationId,
		UUID iikoPaymentTypeId,
		@Size(max = 128) String payriffMerchantId,
		@Size(max = 512) String payriffSecretKey,
		Boolean serviceChargeEnabled,
		@DecimalMin(value = "0.01") @DecimalMax(value = "100.00") @Digits(integer = 3, fraction = 2)
		BigDecimal serviceChargePercent) {
}
