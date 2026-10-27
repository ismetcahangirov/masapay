package az.masapay.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The reviewed menu the manager approved, saved in one go. */
public record MenuImportRequest(@NotEmpty @Size(max = 50) @Valid List<@NotNull MenuImportCategory> categories) {
}
