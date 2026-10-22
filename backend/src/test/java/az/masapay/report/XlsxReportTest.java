package az.masapay.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.masapay.report.XlsxReport.Column;
import az.masapay.report.XlsxReport.Kind;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxReportTest {

	private static final Instant MADE = Instant.parse("2026-10-22T08:30:00Z");

	private XSSFWorkbook read(byte[] bytes) throws Exception {
		return new XSSFWorkbook(new ByteArrayInputStream(bytes));
	}

	private XlsxReport sample() {
		return new XlsxReport("Əməliyyatlar", "Əməliyyatlar hesabatı", List.of("Dolma · 01.10.2026 - 22.10.2026"),
			List.of(new Column("Tarix", Kind.DATE_TIME), new Column("Masa", Kind.TEXT),
				new Column("Məbləğ", Kind.MONEY), new Column("Say", Kind.COUNT)),
			MADE)
			.row(Instant.parse("2026-10-21T21:30:00Z"), "Masa 1", new BigDecimal("12.50"), 2)
			.row(Instant.parse("2026-10-22T09:00:00Z"), "=HYPERLINK(\"http://evil\")", new BigDecimal("7.25"), 1);
	}

	@Test
	void itHasATitleBlockAndABoldFrozenFilteredHeader() throws Exception {
		try (XSSFWorkbook workbook = read(sample().toBytes())) {
			XSSFSheet sheet = workbook.getSheet("Əməliyyatlar");
			assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Əməliyyatlar hesabatı");
			assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("Dolma");
			// 08:30 UTC is 12:30 in Baku.
			assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).contains("22.10.2026 12:30");

			Row header = sheet.getRow(XlsxReport.HEADER_ROW);
			assertThat(header.getCell(2).getStringCellValue()).isEqualTo("Məbləğ");
			assertThat(workbook.getFontAt(header.getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
			assertThat(sheet.getPaneInformation().getHorizontalSplitPosition())
				.isEqualTo((short) (XlsxReport.HEADER_ROW + 1));
			assertThat(sheet.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A5:D7");
			assertThat(sheet.getColumnWidth(1)).isGreaterThan(10 * 256);
		}
	}

	@Test
	void amountsAreNumbersInManatAndTimesAreBakuDates() throws Exception {
		try (XSSFWorkbook workbook = read(sample().toBytes())) {
			Sheet sheet = workbook.getSheetAt(0);
			Row first = sheet.getRow(XlsxReport.HEADER_ROW + 1);

			Cell money = first.getCell(2);
			assertThat(money.getCellType()).isEqualTo(CellType.NUMERIC);
			assertThat(money.getNumericCellValue()).isEqualTo(12.5);
			assertThat(money.getCellStyle().getDataFormatString()).contains("₼");

			Cell when = first.getCell(0);
			assertThat(DateUtil.isCellDateFormatted(when)).isTrue();
			// 21:30 UTC on the 21st is 01:30 on the 22nd in Baku.
			assertThat(when.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 10, 22, 1, 30));
		}
	}

	@Test
	void theTotalsRowSumsMoneyAndCountsWithFormulas() throws Exception {
		try (XSSFWorkbook workbook = read(sample().toBytes())) {
			Row totals = workbook.getSheetAt(0).getRow(XlsxReport.HEADER_ROW + 3);
			assertThat(totals.getCell(0).getStringCellValue()).isEqualTo("Cəmi");
			assertThat(totals.getCell(2).getCellFormula()).isEqualTo("SUM(C6:C7)");
			assertThat(totals.getCell(3).getCellFormula()).isEqualTo("SUM(D6:D7)");
			workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
			assertThat(totals.getCell(2).getNumericCellValue()).isEqualTo(19.75);
		}
	}

	@Test
	void textThatLooksLikeAFormulaStaysText() throws Exception {
		try (XSSFWorkbook workbook = read(sample().toBytes())) {
			Cell sneaky = workbook.getSheetAt(0).getRow(XlsxReport.HEADER_ROW + 2).getCell(1);
			assertThat(sneaky.getCellType()).isEqualTo(CellType.STRING);
			assertThat(sneaky.getStringCellValue()).startsWith("=HYPERLINK");
		}
	}

	@Test
	void anEmptyReportHasNoTotalsAndAMissingValueIsABlankCell() throws Exception {
		XlsxReport empty = new XlsxReport("Bəxşişlər", "Bəxşiş hesabatı", List.of(),
			List.of(new Column("Ofisiant", Kind.TEXT), new Column("Bəxşiş", Kind.MONEY)), MADE);
		try (XSSFWorkbook workbook = read(empty.toBytes())) {
			assertThat((Object) workbook.getSheetAt(0).getRow(XlsxReport.HEADER_ROW + 1)).isNull();
		}
		XlsxReport withBlank = new XlsxReport("Bəxşişlər", "Bəxşiş hesabatı", List.of(),
			List.of(new Column("Ofisiant", Kind.TEXT), new Column("Bəxşiş", Kind.MONEY)), MADE)
			.row(null, new BigDecimal("1.00"));
		try (XSSFWorkbook workbook = read(withBlank.toBytes())) {
			assertThat(workbook.getSheetAt(0).getRow(XlsxReport.HEADER_ROW + 1).getCell(0).getCellType())
				.isEqualTo(CellType.BLANK);
		}
		assertThatThrownBy(() -> withBlank.row("only one"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
