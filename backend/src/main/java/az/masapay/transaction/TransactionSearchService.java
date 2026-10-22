package az.masapay.transaction;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.report.ReportFile;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.transaction.dto.TransactionPage;
import az.masapay.transaction.dto.TransactionRow;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Searchable, paged list of a restaurant's payments for the admin panel (#45).
 * <p>
 * Dates are calendar days in Asia/Baku and both ends are inclusive, so
 * {@code from=2026-10-01&to=2026-10-31} covers rows created in
 * {@code [2026-10-01T00:00+04:00, 2026-11-01T00:00+04:00)}. Rows are newest first.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransactionSearchService {

	/** Restaurants operate on Baku time; filter days start at local midnight. */
	static final ZoneId ZONE = ZoneId.of("Asia/Baku");

	static final int DEFAULT_SIZE = 20;
	static final int MAX_SIZE = 100;
	/** At most this many rows go into one Excel export (#170). */
	static final int EXPORT_LIMIT = 50_000;
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;

	public TransactionSearchService(TransactionRepository transactionRepository, UserRepository userRepository,
			RestaurantRepository restaurantRepository) {
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.restaurantRepository = restaurantRepository;
	}

	@Transactional(readOnly = true)
	public TransactionPage search(AuthenticatedUser principal, UUID restaurantId, TransactionStatus status,
		SplitType splitType, LocalDate from, LocalDate to, String query, int page, int size) {
		checkAccess(principal, restaurantId);
		TransactionFilter filter = toFilter(restaurantId, status, splitType, from, to, query);
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), clampSize(size),
			Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<TransactionRow> rows = transactionRepository
			.findAll(TransactionSpecifications.matching(filter), pageRequest)
			.map(TransactionRow::of);
		return new TransactionPage(rows.getContent(), rows.getTotalElements(), rows.getNumber(), rows.getSize());
	}

	/**
	 * Every transaction matching the filters, newest first, as a formatted Excel file (#170). Capped
	 * at {@link #EXPORT_LIMIT} rows so one click cannot load a restaurant's whole history at once.
	 */
	@Transactional(readOnly = true)
	public ReportFile excel(AuthenticatedUser principal, UUID restaurantId, TransactionStatus status,
		SplitType splitType, LocalDate from, LocalDate to, String query) {
		checkAccess(principal, restaurantId);
		TransactionFilter filter = toFilter(restaurantId, status, splitType, from, to, query);
		List<TransactionRow> rows = transactionRepository
			.findAll(TransactionSpecifications.matching(filter),
				PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
			.map(TransactionRow::of)
			.getContent();
		String restaurantName = restaurantRepository.findById(restaurantId).map(Restaurant::getName).orElse("");

		List<String> filters = new ArrayList<>();
		if (from != null || to != null) {
			filters.add("Tarix: " + (from == null ? "..." : DAY.format(from)) + " - "
				+ (to == null ? "..." : DAY.format(to)));
		}
		if (status != null) {
			filters.add("Status: " + TransactionsXlsx.STATUS_LABELS.get(status));
		}
		if (splitType != null) {
			filters.add("Ödəniş tipi: " + TransactionsXlsx.SPLIT_LABELS.get(splitType));
		}
		if (filter.query() != null) {
			filters.add("Axtarış: " + filter.query());
		}
		return new ReportFile(TransactionsXlsx.write(rows, restaurantName, filters, Instant.now()),
			List.of(restaurantName, "emeliyyatlar", from == null ? "" : from, to == null ? "" : to));
	}

	/** Turns request parameters into criteria: Baku days to instants, blank text to none. */
	static TransactionFilter toFilter(UUID restaurantId, TransactionStatus status, SplitType splitType,
		LocalDate from, LocalDate to, String query) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
		}
		String text = query == null || query.isBlank() ? null : query.strip();
		return new TransactionFilter(restaurantId, status, splitType,
			from == null ? null : startOfDay(from),
			to == null ? null : startOfDay(to.plusDays(1)),
			text);
	}

	/** Page size between 1 and {@link #MAX_SIZE}; zero or negative falls back to the default. */
	static int clampSize(int size) {
		if (size < 1) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}

	/** SUPER_ADMIN sees every restaurant; a RESTO_MANAGER only the one they belong to. */
	private void checkAccess(AuthenticatedUser principal, UUID restaurantId) {
		if (principal.role() == UserRole.SUPER_ADMIN) {
			return;
		}
		if (principal.role() == UserRole.RESTO_MANAGER) {
			UUID ownRestaurantId = userRepository.findById(principal.id())
				.map(User::getRestaurant)
				.map(Restaurant::getId)
				.orElse(null);
			if (restaurantId.equals(ownRestaurantId)) {
				return;
			}
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this restaurant");
	}

	private static Instant startOfDay(LocalDate date) {
		return date.atStartOfDay(ZONE).toInstant();
	}
}
