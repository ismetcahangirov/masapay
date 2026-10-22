package az.masapay.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** The extras to offer with a dish, in order (#141); an empty list offers nothing. */
public record SuggestionsRequest(@NotNull @Size(max = 5) List<@NotNull UUID> itemIds) {
}
