package az.masapay.dashboard.dto;

import az.masapay.domain.enums.TransactionStatus;

/** Number of transactions in one status. */
public record StatusCount(TransactionStatus status, long count) {
}
