package az.masapay.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class ReportDownloadTest {

	@Test
	void azerbaijaniNamesBecomeSafeFileNames() {
		assertThat(ReportDownload.slug("Dolma Restoran")).isEqualTo("dolma-restoran");
		assertThat(ReportDownload.slug("Şəki Çay Evi - Gəncə")).isEqualTo("seki-cay-evi-gence");
		assertThat(ReportDownload.slug("Ümid/Öz: #1 Bağ")).isEqualTo("umid-oz-1-bag");
		assertThat(ReportDownload.slug("  ")).isEmpty();
	}

	@Test
	void itIsAnXlsxAttachmentNamedFromItsParts() {
		ResponseEntity<byte[]> response = ReportDownload.xlsx(new byte[] {1, 2}, "Dolma Restoran", "bexsisler",
			LocalDate.of(2026, 10, 1), "", LocalDate.of(2026, 10, 22));

		assertThat(response.getHeaders().getContentType()).isEqualTo(ReportDownload.XLSX);
		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
			.isEqualTo("attachment; filename=\"dolma-restoran-bexsisler-2026-10-01-2026-10-22.xlsx\"");
		assertThat(response.getBody()).containsExactly(1, 2);
	}
}
