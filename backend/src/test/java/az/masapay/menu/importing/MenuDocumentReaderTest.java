package az.masapay.menu.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.masapay.menu.MenuImagesTest;
import az.masapay.menu.importing.MenuDocumentReader.SourceLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MenuDocumentReaderTest {

	/** OCR that must not be needed. */
	private static final MenuOcr NO_OCR = image -> {
		throw new AssertionError("OCR was not expected");
	};

	private static byte[] pdf(String... lines) throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream text = new PDPageContentStream(document, page)) {
				text.beginText();
				text.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				text.setLeading(16);
				text.newLineAtOffset(50, 700);
				for (String line : lines) {
					text.showText(line);
					text.newLine();
				}
				text.endText();
			}
			document.save(out);
			return out.toByteArray();
		}
	}

	private static byte[] blankPdf(int pages) throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			for (int i = 0; i < pages; i++) {
				document.addPage(new PDPage());
			}
			document.save(out);
			return out.toByteArray();
		}
	}

	@Test
	void aPdfWithTextIsReadLineByLineWithoutOcr() throws IOException {
		MenuDocumentReader.Read read = MenuDocumentReader.read(
			pdf("SALADS", "Caesar ........ 9 AZN", "Greek salad ..... 7 AZN"), NO_OCR);

		assertThat(read.ocr()).isFalse();
		assertThat(read.lines()).extracting(SourceLine::text)
			.containsExactly("SALADS", "Caesar ........ 9 AZN", "Greek salad ..... 7 AZN");
	}

	@Test
	void aScannedPdfIsReadPageByPageWithOcr() throws IOException {
		List<byte[]> pictures = new ArrayList<>();
		MenuOcr ocr = image -> {
			pictures.add(image);
			return pictures.size() == 1 ? "SALATLAR\nSezar 9 AZN\n" : "İÇKİLƏR\nAyran 2\n";
		};

		MenuDocumentReader.Read read = MenuDocumentReader.read(blankPdf(2), ocr);

		assertThat(read.ocr()).isTrue();
		assertThat(read.lines()).extracting(SourceLine::text)
			.containsExactly("SALATLAR", "Sezar 9 AZN", "İÇKİLƏR", "Ayran 2");
		// Every page went to OCR as a PNG drawn at 300 dpi.
		assertThat(pictures).hasSize(2).allSatisfy(picture -> assertThat(picture[1]).isEqualTo((byte) 'P'));
	}

	@Test
	void aScanWhereOcrFindsNothingSaysSo() throws IOException {
		byte[] pdf = blankPdf(1);

		assertThatThrownBy(() -> MenuDocumentReader.read(pdf, image -> "  \n"))
			.isInstanceOfSatisfying(ResponseStatusException.class, e -> {
				assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
				assertThat(e.getReason()).isEqualTo("NO_TEXT");
			});
	}

	@Test
	void aLongScanIsRefusedBeforeAnyOcr() throws IOException {
		byte[] pdf = blankPdf(MenuDocumentReader.MAX_OCR_PAGES + 1);

		assertThatThrownBy(() -> MenuDocumentReader.read(pdf, NO_OCR))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getReason()).isEqualTo("TOO_MANY_PAGES"));
	}

	@Test
	void aPhotoOfAMenuIsReadWithOcr() throws IOException {
		byte[] photo = MenuImagesTest.png(30, 30);

		MenuDocumentReader.Read read = MenuDocumentReader.read(photo, image -> {
			assertThat(image).isEqualTo(photo);
			return "Desertlər\nPaxlava 3\n";
		});

		assertThat(read.ocr()).isTrue();
		assertThat(read.lines()).extracting(SourceLine::text).containsExactly("Desertlər", "Paxlava 3");
	}

	@Test
	void aDocxKeepsParagraphsTableCellsAndPictures() throws Exception {
		byte[] photo = MenuImagesTest.png(20, 20);
		byte[] docx;
		try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			document.createParagraph().createRun().setText("Şorbalar");
			XWPFTable table = document.createTable(1, 3);
			XWPFTableRow row = table.getRow(0);
			row.getCell(0).setText("Mərci");
			row.getCell(1).setText("limon ilə");
			row.getCell(2).setText("4,50");
			row.getCell(0).getParagraphs().get(0).createRun().addPicture(new ByteArrayInputStream(photo),
				Document.PICTURE_TYPE_PNG, "m.png", Units.toEMU(20), Units.toEMU(20));
			document.write(out);
			docx = out.toByteArray();
		}

		MenuDocumentReader.Read read = MenuDocumentReader.read(docx, NO_OCR);

		assertThat(read.ocr()).isFalse();
		assertThat(read.lines().get(0).text()).isEqualTo("Şorbalar");
		assertThat(read.lines().get(1).cells()).containsExactly("Mərci", "limon ilə", "4,50");
		assertThat(read.lines().get(1).picture()).isEqualTo(photo);
	}

	@Test
	void anythingElseIsNotAMenuFile() {
		assertThatThrownBy(() -> MenuDocumentReader.read("Plov 12".getBytes(), NO_OCR))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
	}
}
