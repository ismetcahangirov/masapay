package az.masapay.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** The line items a diner selected to pay for (item-based split, #25). */
public record ItemPaymentRequest(@NotEmpty List<UUID> itemIds) {
}
