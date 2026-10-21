package az.masapay.transaction.dto;

import java.util.List;

/** A page of the admin transactions table; {@code page} is zero-based. */
public record TransactionPage(List<TransactionRow> items, long totalElements, int page, int size) {
}
