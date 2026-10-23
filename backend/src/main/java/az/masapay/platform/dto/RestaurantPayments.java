package az.masapay.platform.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Successful payments of one restaurant in a period (#149).
 *
 * @param revenue bill parts paid, excluding tips
 * @param tips tips paid on top
 */
public record RestaurantPayments(UUID restaurantId, long paymentCount, BigDecimal revenue, BigDecimal tips) {
}
