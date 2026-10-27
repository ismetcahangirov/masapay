package az.masapay.menu.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * The real Tesseract program. CI installs it with the Azerbaijani data; on a machine without it
 * the reading test is skipped.
 */
class TesseractOcrTest {

	private static boolean installed() {
		try {
			Process process = new ProcessBuilder("tesseract", "--list-langs").redirectErrorStream(true).start();
			String langs = new String(process.getInputStream().readAllBytes());
			return process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0 && langs.contains("aze");
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static byte[] menuPhoto(String... lines) throws IOException {
		BufferedImage image = new BufferedImage(1400, 140 + lines.length * 90, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 56));
		for (int i = 0; i < lines.length; i++) {
			g.drawString(lines[i], 70, 120 + i * 90);
		}
		g.dispose();
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(image, "png", png);
		return png.toByteArray();
	}

	@Test
	void readsAPhotoOfAMenuIntoDishesWithPrices() throws IOException {
		assumeTrue(installed(), "tesseract with Azerbaijani data is not installed");
		TesseractOcr ocr = new TesseractOcr("tesseract", "aze+eng+rus", "6", Duration.ofSeconds(60), 1);

		String text = ocr.read(menuPhoto("SALATLAR", "Sezar .............. 9 AZN", "Plov ............... 12 AZN"));

		var menu = MenuTextParser.parse(text.lines().map(MenuDocumentReader.SourceLine::of).toList());
		assertThat(menu).singleElement().satisfies(category -> {
			assertThat(category.name()).isEqualTo("Salatlar");
			assertThat(category.dishes()).extracting(MenuTextParser.Dish::name).containsExactly("Sezar", "Plov");
			assertThat(category.dishes()).extracting(dish -> dish.price().intValue()).containsExactly(9, 12);
		});
	}

	@Test
	void aServerWithoutTesseractSaysOcrIsUnavailable() {
		TesseractOcr ocr = new TesseractOcr("no-such-ocr-program", "aze", "6", Duration.ofSeconds(5), 1);

		assertThatThrownBy(() -> ocr.read(new byte[] {1, 2, 3}))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getReason()).isEqualTo("OCR_UNAVAILABLE"));
	}
}
