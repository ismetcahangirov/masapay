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
		return from(restaurant, true);
	}

	/**
	 * {@code withPayriff} false leaves the Payriff fields out: they are the platform's, set by the
	 * super admin only, and a restaurant's manager does not see them.
	 */
	public static IntegrationSettings from(Restaurant restaurant, boolean withPayriff) {
		return new IntegrationSettings(
			restaurant.getId(),
			restaurant.getPosType(),
			SecretMasker.mask(restaurant.getIikoApiLogin()),
			hasValue(restaurant.getIikoApiLogin()),
			restaurant.getIikoOrganizationId(),
			restaurant.getIikoPaymentTypeId(),
			withPayriff ? restaurant.getPayriffMerchantId() : null,
			withPayriff ? SecretMasker.mask(restaurant.getPayriffSecretKey()) : null,
			withPayriff && hasValue(restaurant.getPayriffSecretKey()),
			restaurant.getServiceChargePercent());
	}

	private static boolean hasValue(String value) {
		return value != null && !value.isEmpty();
	}
}
