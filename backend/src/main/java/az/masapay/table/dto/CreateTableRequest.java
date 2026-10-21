package az.masapay.table.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new table: its label as printed under the QR code, e.g. "Masa 7" or "Terras 2". */
public record CreateTableRequest(@NotBlank @Size(max = 50) String label) {
}
