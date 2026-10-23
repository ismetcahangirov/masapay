package az.masapay.platform;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.UserRole;
import az.masapay.platform.dto.PlatformAnalytics;
import az.masapay.platform.dto.RestaurantPayments;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.restaurant.RestaurantGate;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The super admin's view of the whole platform (#149): turnover, payments, tips and subscription
 * income per restaurant over a range of Baku days, with totals per currency.
 * <p>
 * A day {@code d} covers {@code [d 00:00, d+1 00:00)} Baku time, as on the restaurant dashboard.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAnalyticsService {

	public static final int DEFAULT_DAYS = 30;
	public static final int MAX_DAYS = 366;

	/** A monthly fee is spread over this many days. */
	private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);

	private final RestaurantRepository restaurantRepository;
	private final TransactionRepository transactionRepository;
	private final Clock clock;

	@Autowired
	public PlatformAnalyticsService(RestaurantRepository restaurantRepository,
			TransactionRepository transactionRepository) {
		this(restaurantRepository, transactionRepository, Clock.systemUTC());
	}

	PlatformAnalyticsService(RestaurantRepository restaurantRepository, TransactionRepository transactionRepository,
			Clock clock) {
		this.restaurantRepository = restaurantRepository;
		this.transactionRepository = transactionRepository;
		this.clock = clock;
	}

	/**
	 * @param from first day, included; null means {@value #DEFAULT_DAYS} days back from {@code to}
	 * @param to last day, included; null means today
	 */
	@Transactional(readOnly = true)
	public PlatformAnalytics analytics(LocalDate from, LocalDate to, AuthenticatedUser caller) {
		if (caller.role() != UserRole.SUPER_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super admins only");
		}
		LocalDate today = LocalDate.now(clock.withZone(RestaurantGate.ZONE));
		LocalDate last = to != null ? to : today;
		LocalDate first = from != null ? from : last.minusDays(DEFAULT_DAYS - 1L);
		if (first.isAfter(last)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' is after 'to'");
		}
		if (ChronoUnit.DAYS.between(first, last) + 1 > MAX_DAYS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_DAYS + " days");
		}

		Map<UUID, RestaurantPayments> payments = transactionRepository
			.sumSuccessfulPaymentsByRestaurant(startOfDay(first), startOfDay(last.plusDays(1)))
			.stream()
			.collect(Collectors.toMap(RestaurantPayments::restaurantId, Function.identity()));

		List<PlatformAnalytics.Row> rows = restaurantRepository.findAll().stream()
			.map(restaurant -> row(restaurant, payments.get(restaurant.getId()), first, last, today))
			.sorted(Comparator.comparing(PlatformAnalytics.Row::revenue).reversed()
				.thenComparing(PlatformAnalytics.Row::name, String.CASE_INSENSITIVE_ORDER))
			.toList();

		return new PlatformAnalytics(first, last, RestaurantGate.ZONE.getId(), totals(rows), rows);
	}

	private static PlatformAnalytics.Row row(Restaurant restaurant, RestaurantPayments paid, LocalDate first,
			LocalDate last, LocalDate today) {
		long count = paid == null ? 0 : paid.paymentCount();
		BigDecimal revenue = paid == null ? BigDecimal.ZERO : orZero(paid.revenue());
		BigDecimal tips = paid == null ? BigDecimal.ZERO : orZero(paid.tips());
		BigDecimal average = count == 0 ? null
			: revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
		return new PlatformAnalytics.Row(restaurant.getId(), restaurant.getName(), restaurant.getCurrency(),
			restaurant.isActive(), restaurant.subscriptionStatusOn(today),
			count, revenue, tips, average, subscriptionRevenue(restaurant, first, last));
	}

	/** The monthly fee for every paid subscription day inside {@code [first, last]}, fee / 30 a day. */
	static BigDecimal subscriptionRevenue(Restaurant restaurant, LocalDate first, LocalDate last) {
		if (restaurant.getSubscriptionPlan() == null || restaurant.isSubscriptionTrial()
				|| restaurant.getSubscriptionMonthlyFee() == null) {
			return BigDecimal.ZERO.setScale(2);
		}
		LocalDate start = max(first, restaurant.getSubscriptionStartsOn());
		LocalDate end = min(last, restaurant.getSubscriptionEndsOn());
		long days = start.isAfter(end) ? 0 : ChronoUnit.DAYS.between(start, end) + 1;
		return restaurant.getSubscriptionMonthlyFee()
			.multiply(BigDecimal.valueOf(days))
			.divide(DAYS_PER_MONTH, 2, RoundingMode.HALF_UP);
	}

	/** One line per currency, in currency order; amounts in different currencies are kept apart. */
	private static List<PlatformAnalytics.Totals> totals(List<PlatformAnalytics.Row> rows) {
		Map<String, List<PlatformAnalytics.Row>> byCurrency = new TreeMap<>();
		rows.forEach(row -> byCurrency.computeIfAbsent(row.currency(), c -> new ArrayList<>()).add(row));
		List<PlatformAnalytics.Totals> totals = new ArrayList<>();
		byCurrency.forEach((currency, list) -> totals.add(new PlatformAnalytics.Totals(currency, list.size(),
			(int) list.stream().filter(PlatformAnalytics.Row::active).count(),
			list.stream().mapToLong(PlatformAnalytics.Row::paymentCount).sum(),
			sum(list, PlatformAnalytics.Row::revenue), sum(list, PlatformAnalytics.Row::tips),
			sum(list, PlatformAnalytics.Row::subscriptionRevenue))));
		return totals;
	}

	private static BigDecimal sum(List<PlatformAnalytics.Row> rows,
			Function<PlatformAnalytics.Row, BigDecimal> field) {
		return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static LocalDate max(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? a : b;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

	private static Instant startOfDay(LocalDate day) {
		return day.atStartOfDay(RestaurantGate.ZONE).toInstant();
	}

	private static BigDecimal orZero(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}
}
