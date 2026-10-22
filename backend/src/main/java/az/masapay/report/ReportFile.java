package az.masapay.report;

import java.util.List;
import org.springframework.http.ResponseEntity;

/** A finished report and the words its file is named from, e.g. restaurant, report and period (#170). */
public record ReportFile(byte[] body, List<Object> nameParts) {

	public ResponseEntity<byte[]> asXlsxDownload() {
		return ReportDownload.xlsx(body, nameParts.toArray());
	}
}
