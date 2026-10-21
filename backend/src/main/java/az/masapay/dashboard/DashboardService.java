package az.masapay.dashboard;

import az.masapay.dashboard.dto.DailyRevenue;
import az.masapay.dashboard.dto.DashboardMetrics;
import az.masapay.dashboard.dto.PaidAmount;
import az.masapay.dashboard.dto.PaymentTotals;
import az.masapay.dashboard.dto.StatusCount;
import az.masapay.dashboard.dto.TableVisit;
import az.masapay.domain.Restaurant;
import az.masapay.domain.User;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Builds the admin dashboard KPIs of a restaurant (#43).
 * <p>
 * Days are Asia/Baku calendar days; a day {@code d} covers the half-open range
 * {@code [d 00:00, d+1 00:00)} in that zone, so every instant falls into exactly one day.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DashboardService {

	/** Restaurants operate on Baku time; dashboard days start at local midnight. */
	public static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Baku");

	public static final int DEFAULT_DAYS = 7;
	public static final int MAX_DAYS = 31;

	private final TransactionRepository transactionRepository;
	private final OrderRepository orderRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public DashboardService(TransactionRepository transactionRepository, OrderRepository orderRepository,
		RestaurantRepository restaurantRepository, UserRepository userRepository) {
		this(transactionRepository, orderRepository, restaurantRepository, userRepository, Clock.systemUTC());
	}

	DashboardService(TransactionRepository transactionRepository, OrderRepository orderRepository,
		RestaurantRepository restaurantRepository, UserRepository userRepository, Clock clock) {
		this.transactionRepository = transactionRepository;
		this.orderRepository = orderRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/**
	 * Today's KPIs plus revenue and tips per day for the last {@code days} days (today included).
	 *
	 * @param days length of the chart series, 1..{@value #MAX_DAYS}
	 */
	@Transactional(readOnly = true)
	public DashboardMetrics metrics(AuthenticatedUser principal, UUID restaurantId, int days) {
		if (days < 1 || days > MAX_DAYS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'days' must be between 1 and " + MAX_DAYS);
		}
		checkAccess(principal, restaurantId);
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

		LocalDate today = LocalDate.now(clock.withZone(DASHBOARD_ZONE));
		Instant todayStart = startOfDay(today);
		Instant tomorrowStart = startOfDay(today.plusDays(1));

		PaymentTotals totals = transactionRepository.sumSuccessfulPayments(restaurantId, todayStart, tomorrowStart);
		Double averageMinutes = averageMinutes(orderRepository.findPaidVisits(restaurantId, todayStart, tomorrowStart));

		long succeeded = 0;
		long failed = 0;
		for (StatusCount row : transactionRepository.countPayriffByStatus(restaurantId, todayStart, tomorrowStart)) {
			if (row.status() == TransactionStatus.SUCCESS) {
				succeeded = row.count();
			} else if (row.status() == TransactionStatus.FAILED) {
				failed = row.count();
			}
		}
		Double successRate = succeeded + failed == 0 ? null : (double) succeeded / (succeeded + failed);

		LocalDate firstDay = today.minusDays(days - 1L);
		List<DailyRevenue> series = dailySeries(firstDay, today,
			transactionRepository.findSuccessfulPayments(restaurantId, startOfDay(firstDay), tomorrowStart));

		return new DashboardMetrics(restaurantId, restaurant.getCurrency(), today, DASHBOARD_ZONE.getId(),
			orZero(totals == null ? null : totals.revenue()), orZero(totals == null ? null : totals.tips()),
			averageMinutes, successRate, succeeded, failed, series);
	}

	/** Mean sitting time in minutes, or null when no table was paid. */
	private static Double averageMinutes(List<TableVisit> visits) {
		if (visits.isEmpty()) {
			return null;
		}
		long totalSeconds = 0;
		for (TableVisit visit : visits) {
			totalSeconds += Duration.between(visit.openedAt(), visit.closedAt()).toSeconds();
		}
		return totalSeconds / 60.0 / visits.size();
	}

	/** One entry per day from {@code first} to {@code last}; days without payments are zero. */
	private static List<DailyRevenue> dailySeries(LocalDate first, LocalDate last, List<PaidAmount> payments) {
		Map<LocalDate, BigDecimal[]> byDay = new LinkedHashMap<>();
		for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
			byDay.put(day, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
		}
		for (PaidAmount payment : payments) {
			BigDecimal[] sums = byDay.get(LocalDate.ofInstant(payment.paidAt(), DASHBOARD_ZONE));
			if (sums != null) {
				sums[0] = sums[0].add(orZero(payment.amount()));
				sums[1] = sums[1].add(orZero(payment.tipAmount()));
			}
		}
		List<DailyRevenue> series = new ArrayList<>(byDay.size());
		byDay.forEach((day, sums) -> series.add(new DailyRevenue(day, sums[0], sums[1])));
		return series;
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
		return date.atStartOfDay(DASHBOARD_ZONE).toInstant();
	}

	private static BigDecimal orZero(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}
}
