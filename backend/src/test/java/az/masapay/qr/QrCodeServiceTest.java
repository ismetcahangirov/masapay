package az.masapay.qr;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class QrCodeServiceTest {

	private static final Pattern VIEW_BOX = Pattern.compile("viewBox=\"0 0 (\\d+) (\\d+)\"");
	private static final Pattern RUN = Pattern.compile("M(\\d+) (\\d+)h(\\d+)v1h-\\d+z");
	/** Pixels per module when rasterising the SVG for the decoder. */
	private static final int SCALE = 4;

	private final QrCodeService service = new QrCodeService();
	private final TableQrUrlBuilder urlBuilder = new TableQrUrlBuilder("https://masapay.az/");

	private RestaurantTable table() {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(UUID.randomUUID());
		RestaurantTable table = new RestaurantTable();
		table.setId(UUID.randomUUID());
		table.setRestaurant(restaurant);
		table.setLabel("Masa 7");
		return table;
	}

	@Test
	void urlPointsAtThePwaTableRouteWithTheQrToken() {
		RestaurantTable table = table();

		assertThat(urlBuilder.urlFor(table)).isEqualTo("https://masapay.az/r/" + table.getRestaurant().getId()
			+ "/t/" + table.getId() + "?token=" + table.getQrToken());
	}

	@Test
	void renderedSvgDecodesBackToTheTableUrl() throws Exception {
		RestaurantTable table = table();
		String expected = urlBuilder.urlFor(table);

		String svg = service.renderSvg(expected, table.getLabel());

		assertThat(decode(svg)).isEqualTo(expected);
		assertThat(decode(svg)).contains("token=" + table.getQrToken());
	}

	@Test
	void svgIsWellFormedAndCarriesTheLabel() throws Exception {
		String svg = service.renderSvg("https://masapay.az/x", "Terrace 3");

		Document doc = parse(svg);
		Element root = doc.getDocumentElement();
		assertThat(root.getTagName()).isEqualTo("svg");
		assertThat(root.getAttribute("viewBox")).startsWith("0 0 ");
		assertThat(doc.getElementsByTagName("text").item(0).getTextContent()).isEqualTo("Terrace 3");
	}

	@Test
	void labelIsXmlEscaped() throws Exception {
		String svg = service.renderSvg("https://masapay.az/x", "<Bar & \"Grill\">");

		assertThat(svg).contains("&lt;Bar &amp; &quot;Grill&quot;&gt;");
		assertThat(parse(svg).getElementsByTagName("text").item(0).getTextContent())
			.isEqualTo("<Bar & \"Grill\">");
	}

	@Test
	void blankLabelOmitsTheTextBand() throws Exception {
		String svg = service.renderSvg("https://masapay.az/x", " ");

		assertThat(parse(svg).getElementsByTagName("text").getLength()).isZero();
		Matcher box = VIEW_BOX.matcher(svg);
		assertThat(box.find()).isTrue();
		assertThat(box.group(1)).isEqualTo(box.group(2));
	}

	private static Document parse(String svg) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder()
			.parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Rasterises the SVG's module path back into pixels and decodes it with ZXing,
	 * so the test checks the SVG a printer would receive, not just the matrix.
	 */
	private static String decode(String svg) throws Exception {
		Matcher box = VIEW_BOX.matcher(svg);
		assertThat(box.find()).isTrue();
		int size = Integer.parseInt(box.group(1));
		boolean[][] dark = new boolean[size][size];
		Matcher run = RUN.matcher(svg);
		while (run.find()) {
			int x = Integer.parseInt(run.group(1));
			int y = Integer.parseInt(run.group(2));
			int length = Integer.parseInt(run.group(3));
			for (int i = 0; i < length; i++) {
				dark[y][x + i] = true;
			}
		}
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new GridLuminanceSource(dark, SCALE)));
		Result result = new QRCodeReader().decode(bitmap, Map.of(DecodeHintType.PURE_BARCODE, Boolean.TRUE));
		return result.getText();
	}

	/** Grey-scale view of a module grid: dark modules are black, the rest white. */
	private static final class GridLuminanceSource extends LuminanceSource {

		private final boolean[][] dark;
		private final int scale;

		GridLuminanceSource(boolean[][] dark, int scale) {
			super(dark[0].length * scale, dark.length * scale);
			this.dark = dark;
			this.scale = scale;
		}

		@Override
		public byte[] getRow(int y, byte[] row) {
			byte[] out = row != null && row.length >= getWidth() ? row : new byte[getWidth()];
			for (int x = 0; x < getWidth(); x++) {
				out[x] = dark[y / scale][x / scale] ? 0 : (byte) 0xFF;
			}
			return out;
		}

		@Override
		public byte[] getMatrix() {
			byte[] matrix = new byte[getWidth() * getHeight()];
			byte[] row = new byte[getWidth()];
			for (int y = 0; y < getHeight(); y++) {
				System.arraycopy(getRow(y, row), 0, matrix, y * getWidth(), getWidth());
			}
			return matrix;
		}
	}
}
