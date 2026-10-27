package az.masapay.menu.importing;

/**
 * Reads the text of a picture: a page of a scanned PDF or a photo of a printed menu. Answers
 * 422 OCR_UNAVAILABLE when the server cannot read pictures at all.
 */
@FunctionalInterface
public interface MenuOcr {

	/** The text of a PNG or JPEG picture, line by line. */
	String read(byte[] image);
}
