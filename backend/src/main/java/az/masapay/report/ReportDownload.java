package az.masapay.report;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** The HTTP side of a report download (#170): content type and a readable, safe file name. */
public final class ReportDownload {

	public static final MediaType XLSX =
		MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private ReportDownload() {
	}

	/** An .xlsx attachment named from the parts, e.g. "dolma-restoran-bexsisler-2026-10-01-2026-10-22.xlsx". */
	public static ResponseEntity<byte[]> xlsx(byte[] body, Object... nameParts) {
		String filename = Arrays.stream(nameParts)
			.map(part -> slug(String.valueOf(part)))
			.filter(part -> !part.isEmpty())
			.collect(Collectors.joining("-")) + ".xlsx";
		return ResponseEntity.ok()
			.contentType(XLSX)
			.header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
			.body(body);
	}

	/**
	 * Lower-case ASCII letters, digits and dashes only, so the name works on every system:
	 * Azerbaijani letters become their plain Latin look-alikes ("Bəxşişlər" -> "bexsisler").
	 */
	static String slug(String text) {
		String latin = text.toLowerCase(Locale.ROOT)
			.replace('ə', 'e').replace('ı', 'i').replace('ş', 's').replace('ç', 'c').replace('ğ', 'g')
			.replace('ö', 'o').replace('ü', 'u');
		String ascii = Normalizer.normalize(latin, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
	}
}
