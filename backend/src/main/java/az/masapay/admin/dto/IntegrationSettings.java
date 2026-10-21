package az.masapay.admin.dto;

import az.masapay.admin.SecretMasker;
import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * POS and Payriff settings of a restaurant as shown in the admin panel. Secrets
 * are masked; the *Set flags tell the client whether a secret is stored at all.
 */
public record IntegrationSettings(
		UUID restaurantId,
		PosType posType,
		String iikoApiLogin,
		boolean iikoApiLoginSet,
		UUID iikoOrganizationId,
		UUID iikoPaymentTypeId,
		String payriffMerchantId,
		String payriffSecretKey,
		boolean payriffSecretKeySet,
		BigDecimal serviceChargePercent) {

	public static IntegrationSettings from(Restaurant restaurant) {
		return new IntegrationSettings(
			restaurant.getId(),
			restaurant.getPosType(),
			SecretMasker.mask(restaurant.getIikoApiLogin()),
			hasValue(restaurant.getIikoApiLogin()),
			restaurant.getIikoOrganizationId(),
			restaurant.getIikoPaymentTypeId(),
			restaurant.getPayriffMerchantId(),
			SecretMasker.mask(restaurant.getPayriffSecretKey()),
			hasValue(restaurant.getPayriffSecretKey()),
			restaurant.getServiceChargePercent());
	}

	private static boolean hasValue(String value) {
		return value != null && !value.isEmpty();
	}
}
