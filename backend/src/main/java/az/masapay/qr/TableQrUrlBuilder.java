package az.masapay.qr;

import az.masapay.domain.RestaurantTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds the customer URL a table QR code encodes. It matches the PWA route
 * {@code /r/:restaurantSlug/t/:tableId?token=<token>}: the restaurant id fills the
 * slug segment and the table's {@code qrToken} is the access capability that the
 * public {@code /api/public/tables/{qrToken}/...} endpoints accept.
 */
@Component
public class TableQrUrlBuilder {

	private final String baseUrl;

	public TableQrUrlBuilder(@Value("${masapay.qr.base-url:http://localhost:5173}") String baseUrl) {
		this.baseUrl = stripTrailingSlash(baseUrl);
	}

	/** Returns the absolute URL encoded in the given table's QR code. */
	public String urlFor(RestaurantTable table) {
		return baseUrl
			+ "/r/" + table.getRestaurant().getId()
			+ "/t/" + table.getId()
			+ "?token=" + table.getQrToken();
	}

	private static String stripTrailingSlash(String url) {
		String trimmed = url.strip();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
