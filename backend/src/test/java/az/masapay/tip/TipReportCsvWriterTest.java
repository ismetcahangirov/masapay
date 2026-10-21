package az.masapay.tip;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.tip.dto.TipReport;
import az.masapay.tip.dto.WaiterTip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TipReportCsvWriterTest {

	private static final UUID WAITER_ID = UUID.fromString("f0000000-0000-0000-0000-000000000003");

	private static TipReport report(List<WaiterTip> waiters, long count, String total) {
		return new TipReport(UUID.randomUUID(), "AZN", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
			"Asia/Baku", waiters, count, new BigDecimal(total));
	}

	@Test
	void writesHeaderRowsAndTotal() {
		String csv = TipReportCsvWriter.write(report(List.of(
			new WaiterTip(WAITER_ID, "Aysel Məmmədova", "aysel@example.com", 3, new BigDecimal("12.5")),
			new WaiterTip(null, null, null, 1, new BigDecimal("2.00"))), 4, "14.50"));

		assertThat(csv).isEqualTo("﻿"
			+ "waiter_id,waiter_name,waiter_email,tip_count,tip_total,currency\r\n"
			+ "f0000000-0000-0000-0000-000000000003,Aysel Məmmədova,aysel@example.com,3,12.50,AZN\r\n"
			+ ",Unassigned,,1,2.00,AZN\r\n"
			+ ",TOTAL,,4,14.50,AZN\r\n");
	}

	@Test
	void emptyReportHasHeaderAndZeroTotal() {
		String csv = TipReportCsvWriter.write(report(List.of(), 0, "0"));

		assertThat(csv).isEqualTo("﻿" + TipReportCsvWriter.HEADER + "\r\n" + ",TOTAL,,0,0.00,AZN\r\n");
	}

	@Test
	void quotesFieldsWithCommasQuotesAndLineBreaks() {
		assertThat(TipReportCsvWriter.escape("Mammadov, Aysel")).isEqualTo("\"Mammadov, Aysel\"");
		assertThat(TipReportCsvWriter.escape("Aysel \"Ace\" M")).isEqualTo("\"Aysel \"\"Ace\"\" M\"");
		assertThat(TipReportCsvWriter.escape("line\nbreak")).isEqualTo("\"line\nbreak\"");
		assertThat(TipReportCsvWriter.escape("plain")).isEqualTo("plain");
		assertThat(TipReportCsvWriter.escape("")).isEmpty();
	}

	@Test
	void neutralizesSpreadsheetFormulas() {
		assertThat(TipReportCsvWriter.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
		assertThat(TipReportCsvWriter.escape("+994")).isEqualTo("'+994");
		assertThat(TipReportCsvWriter.escape("@cmd")).isEqualTo("'@cmd");
	}

	@Test
	void escapesNamesInsideRows() {
		String csv = TipReportCsvWriter.write(report(List.of(
			new WaiterTip(WAITER_ID, "Doe, \"JJ\"", "jj@example.com", 1, new BigDecimal("1"))), 1, "1"));

		assertThat(csv).contains("f0000000-0000-0000-0000-000000000003,\"Doe, \"\"JJ\"\"\",jj@example.com,1,1.00,AZN\r\n");
	}
}
