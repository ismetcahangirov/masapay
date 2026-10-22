package az.masapay.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Create or rename a category; a lower {@code sortOrder} is shown first. */
public record MenuCategoryRequest(@NotBlank @Size(max = 100) String name, @NotNull @Min(0) Integer sortOrder) {
}
