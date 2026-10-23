package az.masapay.menu.dto;

import jakarta.validation.constraints.NotNull;

/** Whether customers are asked about this dish before confirming an order (#185). */
public record ExtraRequest(@NotNull Boolean askBeforeConfirm) {
}
