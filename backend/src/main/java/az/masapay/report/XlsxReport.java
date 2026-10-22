package az.masapay.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Writes a report as a formatted Excel workbook (#170), the same way for every report: a title
 * block (report, restaurant, period, when it was made), a bold header row that stays in view
 * (freeze pane) with filters, amounts as numbers in manat, times as Excel dates in Baku time,
 * column widths that fit, and a totals row with SUM formulas. Rows are streamed to a temporary
 * file, so a long period does not fill the memory.
 *
 * <p>Text always goes into string cells, which Excel never evaluates: a value such as
 * {@code =HYPERLINK(...)} typed by someone stays plain text (no formula injection).
 */
public final class XlsxReport {

	/** What a column holds, which decides its format and whether it is totalled. */
	public enum Kind {
		TEXT, MONEY, COUNT, DATE_TIME
	}

	public record Column(String header, Kind kind) {
	}

	static final ZoneId ZONE = ZoneId.of("Asia/Baku");
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	/** Rows kept in memory before they are flushed to the temporary file. */
	private static final int WINDOW = 200;
	private static final int MAX_WIDTH_CHARS = 60;
	/** The header row; the title block sits above it. */
	public static final int HEADER_ROW = 4;
	/** The first row of data, under the header; tests and callers that read a report start here. */
	public static final int FIRST_DATA_ROW = HEADER_ROW + 1;

	private final String sheetName;
	private final String title;
	private final List<String> subtitle;
	private final List<Column> columns;
	private final List<List<Object>> rows = new ArrayList<>();
	private final Instant createdAt;

	public XlsxReport(String sheetName, String title, List<String> subtitle, List<Column> columns,
			Instant createdAt) {
		this.sheetName = sheetName;
		this.title = title;
		this.subtitle = subtitle;
		this.columns = columns;
		this.createdAt = createdAt;
	}

	/** One data row; values are String, BigDecimal (money), Number (count) or Instant, or null. */
	public XlsxReport row(Object... values) {
		if (values.length != columns.size()) {
			throw new IllegalArgumentException("Expected " + columns.size() + " values, got " + values.length);
		}
		rows.add(List.of(nullsAsEmpty(values)));
		return this;
	}

	public byte[] toBytes() {
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW)) {
			Styles styles = new Styles(workbook);
			SXSSFSheet sheet = workbook.createSheet(sheetName);
			int[] widths = new int[columns.size()];

			// Title block.
			cell(sheet.createRow(0), 0, title, styles.title);
			for (int i = 0; i < subtitle.size(); i++) {
				cell(sheet.createRow(1 + i), 0, subtitle.get(i), styles.meta);
			}
			cell(sheet.createRow(1 + subtitle.size()), 0,
				"Yaradılıb: " + STAMP.format(createdAt.atZone(ZONE)) + " (Bakı vaxtı)", styles.meta);

			// Header.
			Row header = sheet.createRow(HEADER_ROW);
			for (int c = 0; c < columns.size(); c++) {
				cell(header, c, columns.get(c).header(), styles.header);
				widths[c] = columns.get(c).header().length();
			}

			// Data.
			int r = HEADER_ROW + 1;
			for (List<Object> values : rows) {
				Row row = sheet.createRow(r++);
				for (int c = 0; c < columns.size(); c++) {
					widths[c] = Math.max(widths[c], write(row.createCell(c), columns.get(c).kind(), values.get(c),
						styles));
				}
			}
			int lastDataRow = r - 1;

			// Totals.
			if (!rows.isEmpty()) {
				Row totals = sheet.createRow(r);
				cell(totals, 0, "Cəmi", styles.totalLabel);
				for (int c = 1; c < columns.size(); c++) {
					Cell cell = totals.createCell(c);
					Kind kind = columns.get(c).kind();
					if (kind == Kind.MONEY || kind == Kind.COUNT) {
						String col = CellReference.convertNumToColString(c);
						cell.setCellFormula("SUM(" + col + (HEADER_ROW + 2) + ":" + col + (lastDataRow + 1) + ")");
						cell.setCellStyle(kind == Kind.MONEY ? styles.totalMoney : styles.totalCount);
					} else {
						cell.setCellStyle(styles.totalLabel);
					}
				}
			}

			sheet.createFreezePane(0, HEADER_ROW + 1);
			sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, Math.max(HEADER_ROW, lastDataRow), 0,
				columns.size() - 1));
			for (int c = 0; c < columns.size(); c++) {
				// Excel measures widths in 1/256 of a character.
				sheet.setColumnWidth(c, Math.min(MAX_WIDTH_CHARS, widths[c] + 3) * 256);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// close() at the end of this block also deletes the temporary file.
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write the Excel report", e);
		}
	}

	/** Writes one value and returns how many characters wide it shows. */
	private static int write(Cell cell, Kind kind, Object value, Styles styles) {
		if (value instanceof Empty) {
			cell.setCellStyle(styles.text);
			return 0;
		}
		switch (kind) {
			case MONEY -> {
				BigDecimal amount = (BigDecimal) value;
				cell.setCellValue(amount.doubleValue());
				cell.setCellStyle(styles.money);
				return amount.toPlainString().length() + 4;
			}
			case COUNT -> {
				cell.setCellValue(((Number) value).doubleValue());
				cell.setCellStyle(styles.count);
				return value.toString().length();
			}
			case DATE_TIME -> {
				LocalDateTime local = LocalDateTime.ofInstant((Instant) value, ZONE);
				cell.setCellValue(local);
				cell.setCellStyle(styles.dateTime);
				return 16;
			}
			default -> {
				String text = value.toString();
				cell.setCellValue(text);
				cell.setCellStyle(styles.text);
				return text.length();
			}
		}
	}

	private static void cell(Row row, int column, String text, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(text);
		cell.setCellStyle(style);
	}

	/** Marks a missing value; List.of does not take nulls. */
	private enum Empty {
		VALUE
	}

	private static Object[] nullsAsEmpty(Object[] values) {
		Object[] copy = values.clone();
		for (int i = 0; i < copy.length; i++) {
			if (copy[i] == null) {
				copy[i] = Empty.VALUE;
			}
		}
		return copy;
	}

	/** The few styles every report shares; a workbook should create each style once. */
	private static final class Styles {
		final CellStyle title;
		final CellStyle meta;
		final CellStyle header;
		final CellStyle text;
		final CellStyle money;
		final CellStyle count;
		final CellStyle dateTime;
		final CellStyle totalLabel;
		final CellStyle totalMoney;
		final CellStyle totalCount;

		Styles(SXSSFWorkbook workbook) {
			short moneyFormat = workbook.createDataFormat().getFormat("#,##0.00 \"₼\";[Red]-#,##0.00 \"₼\"");
			short countFormat = workbook.createDataFormat().getFormat("#,##0");
			short dateFormat = workbook.createDataFormat().getFormat("dd.mm.yyyy hh:mm");

			Font bold = workbook.createFont();
			bold.setBold(true);
			Font big = workbook.createFont();
			big.setBold(true);
			big.setFontHeightInPoints((short) 14);
			Font grey = workbook.createFont();
			grey.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

			title = workbook.createCellStyle();
			title.setFont(big);
			meta = workbook.createCellStyle();
			meta.setFont(grey);

			header = workbook.createCellStyle();
			header.setFont(bold);
			header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			header.setBorderBottom(BorderStyle.THIN);
			header.setAlignment(HorizontalAlignment.LEFT);

			text = workbook.createCellStyle();
			money = workbook.createCellStyle();
			money.setDataFormat(moneyFormat);
			count = workbook.createCellStyle();
			count.setDataFormat(countFormat);
			dateTime = workbook.createCellStyle();
			dateTime.setDataFormat(dateFormat);
			dateTime.setAlignment(HorizontalAlignment.LEFT);

			totalLabel = workbook.createCellStyle();
			totalLabel.setFont(bold);
			totalLabel.setBorderTop(BorderStyle.THIN);
			totalMoney = workbook.createCellStyle();
			totalMoney.cloneStyleFrom(money);
			totalMoney.setFont(bold);
			totalMoney.setBorderTop(BorderStyle.THIN);
			totalCount = workbook.createCellStyle();
			totalCount.cloneStyleFrom(count);
			totalCount.setFont(bold);
			totalCount.setBorderTop(BorderStyle.THIN);
		}
	}
}
