package az.masapay.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MenuImagesTest {

	static byte[] png(int width, int height) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
		return out.toByteArray();
	}

	@Test
	void aBigPngIsShrunkToAJpegOf1024OnTheLongSide() throws IOException {
		MenuImages.Image image = MenuImages.normalise(png(3000, 1500));

		assertThat(image.contentType()).isEqualTo("image/jpeg");
		BufferedImage stored = ImageIO.read(new ByteArrayInputStream(image.data()));
		assertThat(stored.getWidth()).isEqualTo(1024);
		assertThat(stored.getHeight()).isEqualTo(512);
	}

	@Test
	void aSmallPhotoKeepsItsSize() throws IOException {
		MenuImages.Image image = MenuImages.normalise(png(300, 200));

		BufferedImage stored = ImageIO.read(new ByteArrayInputStream(image.data()));
		assertThat(stored.getWidth()).isEqualTo(300);
	}

	@Test
	void somethingThatIsNotAPhotoIsRefused() {
		assertThat(MenuImages.normalise("<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8))).isNull();
		assertThat(MenuImages.normalise(new byte[0])).isNull();
		assertThat(MenuImages.normalise(null)).isNull();
	}

	@Test
	void aPhotoComesBackFromItsDataUrl() throws IOException {
		String url = "data:image/png;base64," + Base64.getEncoder().encodeToString(png(10, 10));

		assertThat(MenuImages.fromDataUrl(url).contentType()).isEqualTo("image/jpeg");
		assertThat(MenuImages.fromDataUrl("data:image/png;base64,@@@")).isNull();
		assertThat(MenuImages.fromDataUrl("https://example.com/a.png")).isNull();
	}
}
