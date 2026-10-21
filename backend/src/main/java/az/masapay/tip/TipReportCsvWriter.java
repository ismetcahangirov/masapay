package az.masapay.tip;

import az.masapay.tip.dto.TipReport;
import az.masapay.tip.dto.WaiterTip;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link TipReport} as RFC 4180 CSV: comma separated, CRLF line ends, a
 * header row, one row per waiter and a closing TOTAL row.
 * <p>
 * A field holding a comma, quote or line break is wrapped in double quotes with inner
 * quotes doubled. Text starting with {@code = + - @} is prefixed with an apostrophe so
 * spreadsheets do not run it as a formula (CSV injection; names come from Google).
 * The output starts with a UTF-8 byte order mark so Excel shows Azerbaijani letters.
 */
public final class TipReportCsvWriter {

	static final String BOM = "﻿";
	static final String HEADER = "waiter_id,waiter_name,waiter_email,tip_count,tip_total,currency";
	static final String UNASSIGNED = "Unassigned";
	private static final String CRLF = "\r\n";

	private TipReportCsvWriter() {
	}

	public static String write(TipReport report) {
		StringBuilder csv = new StringBuilder(BOM).append(HEADER).append(CRLF);
		for (WaiterTip waiter : report.waiters()) {
			String name = waiter.waiterId() == null ? UNASSIGNED : waiter.waiterName();
			appendRow(csv, List.of(
				waiter.waiterId() == null ? "" : waiter.waiterId().toString(),
				nullToEmpty(name),
				nullToEmpty(waiter.waiterEmail()),
				Long.toString(waiter.tipCount()),
				money(waiter.tipTotal()),
				report.currency()));
		}
		appendRow(csv, List.of("", "TOTAL", "", Long.toString(report.totalTipCount()),
			money(report.totalTipAmount()), report.currency()));
		return csv.toString();
	}

	/** Quotes and neutralizes one field for CSV output. */
	static String escape(String value) {
		String field = value;
		if (!field.isEmpty() && "=+-@".indexOf(field.charAt(0)) >= 0) {
			field = "'" + field;
		}
		if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
			return "\"" + field.replace("\"", "\"\"") + "\"";
		}
		return field;
	}

	private static void appendRow(StringBuilder csv, List<String> fields) {
		List<String> escaped = new ArrayList<>(fields.size());
		for (String field : fields) {
			escaped.add(escape(field));
		}
		csv.append(String.join(",", escaped)).append(CRLF);
	}

	private static String money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
