package az.masapay.order.dto;

/** Progress of an in-flight equal split (#26), so every diner sees the same part count. */
public record EqualSplitView(int totalParts, int paidParts) {
}
