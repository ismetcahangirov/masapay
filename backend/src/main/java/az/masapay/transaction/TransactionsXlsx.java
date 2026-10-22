package az.masapay.transaction;

import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.report.XlsxReport;
import az.masapay.report.XlsxReport.Column;
import az.masapay.report.XlsxReport.Kind;
import az.masapay.transaction.dto.TransactionRow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The admin transactions list as a formatted Excel workbook (#170), with the filters it was cut by. */
final class TransactionsXlsx {

	static final Map<SplitType, String> SPLIT_LABELS =
		Map.of(SplitType.FULL, "Tam", SplitType.ITEM, "Məhsula görə", SplitType.EQUAL, "Bərabər");
	static final Map<TransactionStatus, String> STATUS_LABELS = Map.of(TransactionStatus.PENDING, "Gözləyir",
		TransactionStatus.SUCCESS, "Uğurlu", TransactionStatus.FAILED, "Uğursuz", TransactionStatus.REFUNDED,
		"Geri qaytarılıb");

	private TransactionsXlsx() {
	}

	/** {@code filters} describes the cut, e.g. "Status: Uğurlu", shown under the title. */
	static byte[] write(List<TransactionRow> rows, String restaurantName, List<String> filters, Instant now) {
		List<String> subtitle = new ArrayList<>();
		subtitle.add(restaurantName);
		if (!filters.isEmpty()) {
			subtitle.add(String.join(" · ", filters));
		}
		XlsxReport xlsx = new XlsxReport("Əməliyyatlar", "Əməliyyatlar hesabatı", subtitle,
			List.of(new Column("Tarix", Kind.DATE_TIME), new Column("Masa", Kind.TEXT),
				new Column("Ödəniş tipi", Kind.TEXT), new Column("Status", Kind.TEXT),
				new Column("Məbləğ", Kind.MONEY), new Column("Bəxşiş", Kind.MONEY), new Column("Cəmi", Kind.MONEY),
				new Column("Geri qaytarılan", Kind.MONEY), new Column("Payriff ID", Kind.TEXT)),
			now);
		for (TransactionRow row : rows) {
			xlsx.row(row.createdAt(), row.tableLabel(),
				row.splitType() == null ? null : SPLIT_LABELS.get(row.splitType()), STATUS_LABELS.get(row.status()),
				row.amount(), row.tipAmount(), row.totalAmount(), row.refundedAmount(), row.payriffOrderId());
		}
		return xlsx.toBytes();
	}
}
