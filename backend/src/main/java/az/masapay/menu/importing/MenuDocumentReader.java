package az.masapay.menu.importing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns a menu file into lines in reading order. PDF, Word .docx, old Word .doc and photos (JPEG,
 * PNG) are told apart by their first bytes, not the file name. A Word table row keeps its cells
 * apart, and a .docx keeps the pictures standing in a paragraph or row, so a photo next to a dish
 * can go with it. A scanned PDF (pictures of pages, no text) and a photo of a printed menu are
 * read with OCR. PDF pictures are not taken: the manager adds them in the review.
 */
public final class MenuDocumentReader {

	/** One paragraph (one cell) or table row (several), with the first picture standing in it. */
	public record SourceLine(List<String> cells, byte[] picture) {

		public static SourceLine of(String text) {
			return new SourceLine(List.of(text), null);
		}

		public String text() {
			return String.join(" ", cells).trim();
		}
	}

	/** The lines, and whether they were recognised from pictures (OCR), so worth a closer look. */
	public record Read(List<SourceLine> lines, boolean ocr) {
	}

	static final int MAX_PDF_PAGES = 60;
	/** OCR takes seconds per page; a longer scanned file is refused rather than kept waiting. */
	static final int MAX_OCR_PAGES = 15;
	/** Fewer letters than this in a PDF means its pages are pictures, such as a scan. */
	static final int MIN_TEXT_LETTERS = 20;
	/** Dots per inch a scanned page is drawn at for OCR; 300 is what Tesseract reads best. */
	static final int OCR_DPI = 300;

	private MenuDocumentReader() {
	}

	public static Read read(byte[] file, MenuOcr ocr) {
		if (startsWith(file, '%', 'P', 'D', 'F')) {
			return pdf(file, ocr);
		}
		if (startsWith(file, 'P', 'K', 3, 4)) {
			return new Read(docx(file), false);
		}
		if (startsWith(file, 0xD0, 0xCF, 0x11, 0xE0)) {
			return new Read(doc(file), false);
		}
		if (startsWith(file, 0xFF, 0xD8, 0xFF) || startsWith(file, 0x89, 'P', 'N', 'G')) {
			return new Read(found(lines(ocr.read(file))), true);
		}
		throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE");
	}

	private static Read pdf(byte[] file, MenuOcr ocr) {
		try (PDDocument document = Loader.loadPDF(file)) {
			if (document.getNumberOfPages() > MAX_PDF_PAGES) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TOO_MANY_PAGES");
			}
			PDFTextStripper stripper = new PDFTextStripper();
			// Two-column menus and price columns come out line by line, as a person reads them.
			stripper.setSortByPosition(true);
			String text = stripper.getText(document);
			if (text.codePoints().filter(Character::isLetter).count() >= MIN_TEXT_LETTERS) {
				return new Read(lines(text), false);
			}
			// A scanned menu: every page is a picture of text, so it is drawn and read with OCR.
			if (document.getNumberOfPages() > MAX_OCR_PAGES) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TOO_MANY_PAGES");
			}
			PDFRenderer renderer = new PDFRenderer(document);
			List<SourceLine> lines = new ArrayList<>();
			for (int page = 0; page < document.getNumberOfPages(); page++) {
				BufferedImage picture = renderer.renderImageWithDPI(page, OCR_DPI, ImageType.GRAY);
				ByteArrayOutputStream png = new ByteArrayOutputStream();
				ImageIO.write(picture, "png", png);
				lines.addAll(lines(ocr.read(png.toByteArray())));
			}
			return new Read(found(lines), true);
		} catch (InvalidPasswordException e) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PROTECTED_FILE");
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "UNREADABLE_FILE");
		}
	}

	private static List<SourceLine> docx(byte[] file) {
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file))) {
			List<SourceLine> lines = new ArrayList<>();
			for (IBodyElement element : document.getBodyElements()) {
				if (element instanceof XWPFParagraph paragraph) {
					addParagraph(lines, paragraph);
				} else if (element instanceof XWPFTable table) {
					for (XWPFTableRow row : table.getRows()) {
						List<String> cells = new ArrayList<>();
						byte[] picture = null;
						for (XWPFTableCell cell : row.getTableCells()) {
							cells.add(clean(cell.getText()));
							for (XWPFParagraph paragraph : cell.getParagraphs()) {
								picture = picture != null ? picture : picture(paragraph);
							}
						}
						lines.add(new SourceLine(cells, picture));
					}
				}
			}
			return lines;
		} catch (IOException | RuntimeException e) {
			// POI throws runtime exceptions for broken or zip-bomb files.
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "UNREADABLE_FILE");
		}
	}

	private static void addParagraph(List<SourceLine> lines, XWPFParagraph paragraph) {
		byte[] picture = picture(paragraph);
		// A soft line break inside a paragraph is a new line to the reader too.
		List<String> texts = Arrays.stream(paragraph.getText().split("\\R|\\u000B"))
			.map(MenuDocumentReader::clean)
			.toList();
		for (int i = 0; i < texts.size(); i++) {
			lines.add(new SourceLine(List.of(texts.get(i)), i == 0 ? picture : null));
		}
	}

	private static byte[] picture(XWPFParagraph paragraph) {
		for (XWPFRun run : paragraph.getRuns()) {
			for (XWPFPicture picture : run.getEmbeddedPictures()) {
				if (picture.getPictureData() != null) {
					return picture.getPictureData().getData();
				}
			}
		}
		return null;
	}

	private static List<SourceLine> doc(byte[] file) {
		try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(file));
				WordExtractor extractor = new WordExtractor(document)) {
			List<SourceLine> lines = new ArrayList<>();
			for (String paragraph : extractor.getParagraphText()) {
				// In .doc a table row is one paragraph whose cells end with the 0x07 mark.
				if (paragraph.indexOf('\u0007') >= 0) {
					List<String> cells = Arrays.stream(paragraph.split("\u0007")).map(MenuDocumentReader::clean).toList();
					lines.add(new SourceLine(cells, null));
				} else {
					lines.addAll(lines(paragraph));
				}
			}
			return lines;
		} catch (IOException | RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "UNREADABLE_FILE");
		}
	}

	/** No text even after OCR: an empty page, or a picture too blurred to read. */
	private static List<SourceLine> found(List<SourceLine> lines) {
		if (lines.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_TEXT");
		}
		return lines;
	}

	private static List<SourceLine> lines(String text) {
		return text.lines().map(MenuDocumentReader::clean).filter(line -> !line.isEmpty()).map(SourceLine::of).toList();
	}

	/** One space between words; tabs become spaces, non-breaking spaces too. */
	static String clean(String text) {
		return text == null ? "" : text.replace(' ', ' ').replaceAll("[\\t\\r\\n\\u000B\\u0007]+", " ")
			.replaceAll(" {2,}", " ").trim();
	}

	private static boolean startsWith(byte[] file, int... magic) {
		if (file == null || file.length < magic.length) {
			return false;
		}
		for (int i = 0; i < magic.length; i++) {
			if ((file[i] & 0xFF) != magic[i]) {
				return false;
			}
		}
		return true;
	}
}
