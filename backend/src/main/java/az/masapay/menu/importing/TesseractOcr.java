package az.masapay.menu.importing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * OCR with the Tesseract program (installed in the backend Docker image with the Azerbaijani,
 * English and Russian language data). It is run as a separate process with fixed arguments and
 * no shell, on a temporary copy of the picture that is deleted afterwards. OCR is heavy on the
 * CPU, so only a few pictures are read at the same time, and a picture that takes too long is
 * given up.
 */
@Component
public class TesseractOcr implements MenuOcr {

	private static final Logger log = LoggerFactory.getLogger(TesseractOcr.class);

	private final String command;
	private final String languages;
	private final String pageMode;
	private final Duration timeout;
	private final Semaphore running;

	public TesseractOcr(@Value("${masapay.menu-import.ocr.command:tesseract}") String command,
			@Value("${masapay.menu-import.ocr.languages:aze+eng+rus}") String languages,
			@Value("${masapay.menu-import.ocr.page-mode:6}") String pageMode,
			@Value("${masapay.menu-import.ocr.timeout:60s}") Duration timeout,
			@Value("${masapay.menu-import.ocr.parallel:2}") int parallel) {
		this.command = command;
		this.languages = languages;
		this.pageMode = pageMode;
		this.timeout = timeout;
		this.running = new Semaphore(Math.max(1, parallel), true);
	}

	@Override
	public String read(byte[] image) {
		boolean acquired;
		try {
			acquired = running.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OCR_BUSY");
		}
		if (!acquired) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OCR_BUSY");
		}
		Path dir = null;
		try {
			dir = Files.createTempDirectory("menu-ocr");
			Path picture = dir.resolve("page");
			Files.write(picture, image);
			// Page mode 6 (one block of text) keeps a dish and its price, across the dot leaders,
			// on one line; the automatic modes split the price column off the names.
			return run(List.of(command, picture.toString(), "stdout", "-l", languages, "--psm", pageMode));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			running.release();
			delete(dir);
		}
	}

	private String run(List<String> arguments) {
		Process process;
		try {
			process = new ProcessBuilder(arguments).redirectError(ProcessBuilder.Redirect.DISCARD).start();
		} catch (IOException e) {
			log.warn("Tesseract could not be started ({}); scanned menus cannot be read", e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_UNAVAILABLE");
		}
		// Read the output while the program runs, so a full pipe never blocks it.
		CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
			try (InputStream stdout = process.getInputStream()) {
				return new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		try {
			if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_TIMEOUT");
			}
			if (process.exitValue() != 0) {
				// Usually missing language data: the server is not set up for OCR.
				log.warn("Tesseract exited with {}; check that the {} language data is installed",
					process.exitValue(), languages);
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_UNAVAILABLE");
			}
			return output.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_TIMEOUT");
		} catch (ExecutionException | TimeoutException e) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "UNREADABLE_FILE");
		}
	}

	private static void delete(Path dir) {
		if (dir == null) {
			return;
		}
		try (Stream<Path> files = Files.walk(dir)) {
			files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		} catch (IOException e) {
			log.debug("Could not delete {}", dir, e);
		}
	}
}
