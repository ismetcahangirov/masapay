package az.masapay.qr;

import az.masapay.domain.RestaurantTable;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Produces printable QR codes for restaurant tables: a single SVG or a ZIP with one
 * SVG per table. A SUPER_ADMIN may export any restaurant; any other caller only the
 * restaurant their account is assigned to (403 otherwise).
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TableQrExportService {

	private final RestaurantTableRepository tableRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final QrCodeService qrCodeService;
	private final TableQrUrlBuilder urlBuilder;

	public TableQrExportService(RestaurantTableRepository tableRepository, RestaurantRepository restaurantRepository,
			UserRepository userRepository, QrCodeService qrCodeService, TableQrUrlBuilder urlBuilder) {
		this.tableRepository = tableRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.qrCodeService = qrCodeService;
		this.urlBuilder = urlBuilder;
	}

	/** Returns the labelled SVG QR code of one table. */
	@Transactional(readOnly = true)
	public String tableSvg(AuthenticatedUser caller, UUID tableId) {
		RestaurantTable table = tableRepository.findById(tableId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		checkAccess(caller, table.getRestaurant().getId());
		return render(table);
	}

	/** Returns a ZIP archive with one SVG QR code per table of the restaurant. */
	@Transactional(readOnly = true)
	public byte[] restaurantZip(AuthenticatedUser caller, UUID restaurantId) {
		checkAccess(caller, restaurantId);
		if (!restaurantRepository.existsById(restaurantId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
		}
		List<RestaurantTable> tables = tableRepository.findByRestaurantIdOrderByLabelAsc(restaurantId);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		Set<String> usedNames = new HashSet<>();
		try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
			for (RestaurantTable table : tables) {
				zip.putNextEntry(new ZipEntry(uniqueName(table, usedNames)));
				zip.write(render(table).getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		} catch (IOException ex) {
			throw new UncheckedIOException("Could not build the QR code archive", ex);
		}
		return bytes.toByteArray();
	}

	private String render(RestaurantTable table) {
		return qrCodeService.renderSvg(urlBuilder.urlFor(table), table.getLabel());
	}

	/** SUPER_ADMIN passes; everyone else must belong to the restaurant. Loads the user for the restaurant id. */
	private void checkAccess(AuthenticatedUser caller, UUID restaurantId) {
		if (caller.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		User user = userRepository.findById(caller.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user"));
		if (user.getRestaurant() == null || !restaurantId.equals(user.getRestaurant().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
	}

	/**
	 * File name from the table label, reduced to safe characters; falls back to the
	 * qrToken when nothing is left, and to label plus token when the name repeats.
	 */
	static String uniqueName(RestaurantTable table, Set<String> usedNames) {
		String base = sanitize(table.getLabel());
		if (base.isEmpty()) {
			base = table.getQrToken().toString();
		}
		String name = base + ".svg";
		if (!usedNames.add(name)) {
			name = base + "-" + table.getQrToken() + ".svg";
			usedNames.add(name);
		}
		return name;
	}

	static String sanitize(String label) {
		if (label == null) {
			return "";
		}
		String safe = label.strip().replaceAll("[^A-Za-z0-9._-]+", "_");
		return safe.replaceAll("^[._]+|_+$", "");
	}
}
