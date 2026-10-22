package az.masapay.tip;

import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.report.ReportFile;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.tip.dto.TipReport;
import az.masapay.tip.dto.WaiterTip;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Builds the per-waiter tip report of a restaurant (#48).
 * <p>
 * Dates are calendar days in {@link #REPORT_ZONE}; both ends are inclusive, so
 * {@code from=2026-10-01&to=2026-10-31} covers payments in
 * {@code [2026-10-01T00:00+04:00, 2026-11-01T00:00+04:00)}. A missing {@code from}
 * defaults to the first day of the current month and a missing {@code to} to today.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TipReportService {

	/** Restaurants operate on Baku time; report days start at local midnight. */
	public static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Baku");

	private final TransactionRepository transactionRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public TipReportService(TransactionRepository transactionRepository,
		RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this(transactionRepository, restaurantRepository, userRepository, Clock.systemUTC());
	}

	TipReportService(TransactionRepository transactionRepository, RestaurantRepository restaurantRepository,
		UserRepository userRepository, Clock clock) {
		this.transactionRepository = transactionRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public TipReport report(AuthenticatedUser principal, UUID restaurantId, LocalDate from, LocalDate to) {
		LocalDate today = LocalDate.now(clock.withZone(REPORT_ZONE));
		LocalDate start = from != null ? from : today.withDayOfMonth(1);
		LocalDate end = to != null ? to : today;
		if (start.isAfter(end)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
		}

		checkAccess(principal, restaurantId);
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

		List<WaiterTip> waiters = transactionRepository.sumTipsByWaiter(
			restaurantId, TransactionStatus.SUCCESS, startOfDay(start), startOfDay(end.plusDays(1)));
		long totalCount = waiters.stream().mapToLong(WaiterTip::tipCount).sum();
		BigDecimal totalAmount = waiters.stream().map(WaiterTip::tipTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

		return new TipReport(restaurantId, restaurant.getCurrency(), start, end, REPORT_ZONE.getId(),
			waiters, totalCount, totalAmount);
	}

	/** The same report as a formatted Excel file (#170), named after the restaurant and the period. */
	@Transactional(readOnly = true)
	public ReportFile excel(AuthenticatedUser principal, UUID restaurantId, LocalDate from, LocalDate to) {
		TipReport report = report(principal, restaurantId, from, to);
		String restaurantName = restaurantRepository.findById(restaurantId).map(Restaurant::getName).orElse("");
		return new ReportFile(TipReportXlsx.write(report, restaurantName, clock.instant()),
			List.of(restaurantName, "bexsisler", report.from(), report.to()));
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
		return date.atStartOfDay(REPORT_ZONE).toInstant();
	}
}
