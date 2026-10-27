package az.masapay.menu.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import az.masapay.menu.MenuImagesTest;
import az.masapay.menu.importing.MenuDocumentReader.SourceLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

	@Test
	void aPdfIsReadLineByLine() throws IOException {
		byte[] pdf;
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream text = new PDPageContentStream(document, page)) {
				text.beginText();
				text.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				text.setLeading(16);
				text.newLineAtOffset(50, 700);
				text.showText("SALADS");
				text.newLine();
				text.showText("Caesar ........ 9 AZN");
				text.endText();
			}
			document.save(out);
			pdf = out.toByteArray();
		}

		assertThat(MenuDocumentReader.read(pdf)).extracting(SourceLine::text)
			.containsExactly("SALADS", "Caesar ........ 9 AZN");
	}

	@Test
	void aPdfWithoutTextIsAScan() throws IOException {
		byte[] pdf;
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			document.addPage(new PDPage());
			document.save(out);
			pdf = out.toByteArray();
		}

		assertThatThrownBy(() -> MenuDocumentReader.read(pdf))
			.isInstanceOfSatisfying(ResponseStatusException.class, e -> {
				assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
				assertThat(e.getReason()).isEqualTo("NO_TEXT");
			});
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

		List<SourceLine> lines = MenuDocumentReader.read(docx);

		assertThat(lines.get(0).text()).isEqualTo("Şorbalar");
		assertThat(lines.get(1).cells()).containsExactly("Mərci", "limon ilə", "4,50");
		assertThat(lines.get(1).picture()).isEqualTo(photo);
	}

	@Test
	void anythingElseIsNotAMenuFile() {
		assertThatThrownBy(() -> MenuDocumentReader.read("Plov 12".getBytes()))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
	}
}
