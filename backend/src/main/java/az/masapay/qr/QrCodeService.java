package az.masapay.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Builds QR codes with ZXing and renders them as standalone, print-ready SVG. The
 * output is pure vector (one unit per module), so it scales to any print size
 * without blurring. Error correction level M tolerates about 15% damage, which
 * covers scratches and stains on a table tent.
 */
@Service
public class QrCodeService {

	/** Quiet zone in modules; the QR specification requires at least 4. */
	static final int QUIET_ZONE = 4;

	/** Height in modules of the band under the code that holds the label text. */
	private static final int LABEL_BAND = 8;

	/** Physical width written into the SVG so it prints at a sensible size by default. */
	private static final int PRINT_WIDTH_MM = 60;

	private final QRCodeWriter writer = new QRCodeWriter();

	/**
	 * Encodes the content into a QR matrix that already includes the quiet zone,
	 * one matrix cell per QR module.
	 */
	public BitMatrix encode(String content) {
		Map<EncodeHintType, Object> hints = Map.of(
			EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
			EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
			EncodeHintType.MARGIN, QUIET_ZONE);
		try {
			// Width/height 0 means "natural size": exactly one cell per module.
			return writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
		} catch (WriterException ex) {
			throw new IllegalArgumentException("Content cannot be encoded as a QR code", ex);
		}
	}

	/**
	 * Renders the content as an SVG document: black modules on a white background,
	 * with the optional label centred underneath. A blank label omits the text band.
	 */
	public String renderSvg(String content, String label) {
		BitMatrix matrix = encode(content);
		int size = matrix.getWidth();
		boolean hasLabel = label != null && !label.isBlank();
		int height = hasLabel ? size + LABEL_BAND : size;
		double heightMm = PRINT_WIDTH_MM * (double) height / size;

		StringBuilder svg = new StringBuilder(8192);
		svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\"")
			.append(" width=\"").append(PRINT_WIDTH_MM).append("mm\"")
			.append(" height=\"").append(String.format(Locale.ROOT, "%.2f", heightMm)).append("mm\"")
			.append(" viewBox=\"0 0 ").append(size).append(' ').append(height).append("\"")
			.append(" shape-rendering=\"crispEdges\">\n")
			.append("<rect width=\"").append(size).append("\" height=\"").append(height)
			.append("\" fill=\"#FFFFFF\"/>\n")
			.append("<path fill=\"#000000\" d=\"").append(modulePath(matrix)).append("\"/>\n");
		if (hasLabel) {
			svg.append("<text x=\"").append(size / 2.0).append("\" y=\"").append(size + LABEL_BAND / 2.0)
				.append("\" font-family=\"Helvetica Neue, Arial, sans-serif\" font-size=\"4\"")
				.append(" text-anchor=\"middle\" dominant-baseline=\"middle\" fill=\"#000000\">")
				.append(escapeXml(label.strip()))
				.append("</text>\n");
		}
		svg.append("</svg>\n");
		return svg.toString();
	}

	/**
	 * One path for all dark modules; horizontal runs are merged into a single
	 * rectangle ("M x y h n v 1 h -n z") to keep the file small.
	 */
	private static String modulePath(BitMatrix matrix) {
		StringBuilder d = new StringBuilder();
		for (int y = 0; y < matrix.getHeight(); y++) {
			int x = 0;
			while (x < matrix.getWidth()) {
				if (!matrix.get(x, y)) {
					x++;
					continue;
				}
				int start = x;
				while (x < matrix.getWidth() && matrix.get(x, y)) {
					x++;
				}
				int run = x - start;
				d.append('M').append(start).append(' ').append(y)
					.append('h').append(run).append("v1h-").append(run).append('z');
			}
		}
		return d.toString();
	}

	/** Escapes the five XML special characters so a label cannot break the document. */
	static String escapeXml(String text) {
		StringBuilder out = new StringBuilder(text.length());
		for (char c : text.toCharArray()) {
			switch (c) {
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append("&quot;");
				case '\'' -> out.append("&apos;");
				default -> out.append(c);
			}
		}
		return out.toString();
	}
}
