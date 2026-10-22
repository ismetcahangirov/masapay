package az.masapay.tip;

import az.masapay.report.XlsxReport;
import az.masapay.report.XlsxReport.Column;
import az.masapay.report.XlsxReport.Kind;
import az.masapay.tip.dto.TipReport;
import az.masapay.tip.dto.WaiterTip;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** The tip report as a formatted Excel workbook (#170): one row per waiter, totals at the bottom. */
final class TipReportXlsx {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private TipReportXlsx() {
	}

	static byte[] write(TipReport report, String restaurantName, Instant now) {
		XlsxReport xlsx = new XlsxReport("Bəxşişlər", "Bəxşiş hesabatı",
			List.of(restaurantName + " · " + DAY.format(report.from()) + " - " + DAY.format(report.to())),
			List.of(new Column("Ofisiant", Kind.TEXT), new Column("Email", Kind.TEXT),
				new Column("Bəxşişli ödənişlər", Kind.COUNT), new Column("Bəxşiş", Kind.MONEY)),
			now);
		for (WaiterTip waiter : report.waiters()) {
			boolean unassigned = waiter.waiterId() == null;
			xlsx.row(unassigned ? "Təyin olunmayıb" : waiter.waiterName(), unassigned ? null : waiter.waiterEmail(),
				waiter.tipCount(), waiter.tipTotal());
		}
		return xlsx.toBytes();
	}
}
