package az.masapay.menu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Dish photos as we store them. The type is taken from the file's first bytes, never from its
 * name or the browser's word, and only JPEG, PNG and WebP are accepted. JPEG and PNG are made at
 * most {@value #MAX_SIDE} px on the long side and saved as JPEG, so a 5 MB phone photo becomes a
 * small one; WebP (which the JDK cannot decode) is kept as it is if small enough.
 */
public final class MenuImages {

	/** A photo as stored: its media type and bytes. */
	public record Image(String contentType, byte[] data) {

		/** As a {@code data:} URL, for the import preview. */
		public String dataUrl() {
			return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data);
		}
	}

	static final int MAX_SIDE = 1024;
	/** Largest stored photo; a bigger WebP is refused rather than shrunk. */
	static final int MAX_STORED_BYTES = 2 * 1024 * 1024;
	/** Largest uploaded file before shrinking. */
	static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

	private MenuImages() {
	}

	/** image/jpeg, image/png or image/webp by the magic bytes; null for anything else. */
	static String detect(byte[] data) {
		if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}
		if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
			return "image/png";
		}
		if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
				&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
			return "image/webp";
		}
		return null;
	}

	/**
	 * The photo ready to store, or null when it is not a JPEG, PNG or WebP we can read, or too big.
	 * A huge picture is read with subsampling, so it never needs much memory.
	 */
	public static Image normalise(byte[] data) {
		if (data == null || data.length == 0 || data.length > MAX_UPLOAD_BYTES) {
			return null;
		}
		String type = detect(data);
		if (type == null) {
			return null;
		}
		if (type.equals("image/webp")) {
			return data.length <= MAX_STORED_BYTES ? new Image(type, data) : null;
		}
		try {
			BufferedImage source = read(data);
			if (source == null) {
				return null;
			}
			byte[] jpeg = writeJpeg(scaled(source));
			return jpeg.length <= MAX_STORED_BYTES ? new Image("image/jpeg", jpeg) : null;
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	/** Decodes a {@code data:image/...;base64,} URL sent back by the import review; null if broken. */
	public static Image fromDataUrl(String dataUrl) {
		if (dataUrl == null || !dataUrl.startsWith("data:")) {
			return null;
		}
		int comma = dataUrl.indexOf(',');
		if (comma < 0 || !dataUrl.substring(0, comma).endsWith(";base64")) {
			return null;
		}
		try {
			return normalise(Base64.getDecoder().decode(dataUrl.substring(comma + 1)));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static BufferedImage read(byte[] data) throws IOException {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				return null;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				int longSide = Math.max(reader.getWidth(0), reader.getHeight(0));
				ImageReadParam param = reader.getDefaultReadParam();
				// Every n-th pixel only, so a 20000 px picture is never fully decoded into memory.
				int step = Math.max(1, longSide / MAX_SIDE);
				param.setSourceSubsampling(step, step, 0, 0);
				return reader.read(0, param);
			} finally {
				reader.dispose();
			}
		}
	}

	/** At most MAX_SIDE on the long side, on white: JPEG has no transparency. */
	private static BufferedImage scaled(BufferedImage source) {
		int longSide = Math.max(source.getWidth(), source.getHeight());
		double factor = longSide > MAX_SIDE ? (double) MAX_SIDE / longSide : 1;
		int width = Math.max(1, (int) Math.round(source.getWidth() * factor));
		int height = Math.max(1, (int) Math.round(source.getHeight() * factor));
		BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = target.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.drawImage(source, 0, 0, width, height, null);
		} finally {
			g.dispose();
		}
		return target;
	}

	private static byte[] writeJpeg(BufferedImage image) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ImageOutputStream output = ImageIO.createImageOutputStream(out)) {
			writer.setOutput(output);
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(0.85f);
			writer.write(null, new IIOImage(image, null, null), param);
		} finally {
			writer.dispose();
		}
		return out.toByteArray();
	}
}
