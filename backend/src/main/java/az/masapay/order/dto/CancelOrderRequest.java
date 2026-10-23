package az.masapay.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why the manager cancels the order (#186), e.g. "Müştəri getdi, heç nə hazırlanmayıb". */
public record CancelOrderRequest(@NotBlank @Size(max = 500) String reason) {
}
