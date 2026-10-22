package az.masapay.platform.dto;

import jakarta.validation.constraints.Size;

/** Rename a restaurant or switch it on / off (#147); null fields stay as they are. */
public record UpdateRestaurantRequest(@Size(min = 1, max = 255) String name, Boolean active) {
}
