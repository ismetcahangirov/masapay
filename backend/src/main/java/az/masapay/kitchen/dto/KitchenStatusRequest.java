package az.masapay.kitchen.dto;

import az.masapay.domain.enums.KitchenStatus;
import jakarta.validation.constraints.NotNull;

/** The chef marks a dish as preparing or ready (or back to new after a mis-tap). */
public record KitchenStatusRequest(@NotNull KitchenStatus status) {
}
