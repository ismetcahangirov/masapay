package az.masapay.tip;

import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.tip.dto.MyTipLine;
import az.masapay.tip.dto.MyTips;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A waiter's own tips with the table each came from (#182). Only the signed-in waiter's tips: the
 * waiter is taken from the session, never from the request, so nobody can read someone else's.
 * Days as in the tip report: Baku calendar days, both ends included; this month by default.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyTipsService {

	public static final int MAX_DAYS = 366;

	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public MyTipsService(TransactionRepository transactionRepository, UserRepository userRepository) {
		this(transactionRepository, userRepository, Clock.systemUTC());
	}

	MyTipsService(TransactionRepository transactionRepository, UserRepository userRepository, Clock clock) {
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public MyTips mine(AuthenticatedUser caller, LocalDate from, LocalDate to) {
		if (caller.role() != UserRole.WAITER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only waiters have their own tips");
		}
		User waiter = userRepository.findById(caller.id())
			.filter(user -> user.getRestaurant() != null)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No restaurant"));
		LocalDate today = LocalDate.now(clock.withZone(TipReportService.REPORT_ZONE));
		LocalDate start = from != null ? from : today.withDayOfMonth(1);
		LocalDate end = to != null ? to : today;
		if (start.isAfter(end)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
		}
		if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_DAYS + " days");
		}

		List<MyTipLine> tips = transactionRepository.findTipsOfWaiter(waiter.getId(), startOfDay(start),
			startOfDay(end.plusDays(1)));
		BigDecimal total = tips.stream().map(MyTipLine::tipAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new MyTips(start, end, TipReportService.REPORT_ZONE.getId(), waiter.getRestaurant().getCurrency(),
			tips.size(), total, tips);
	}

	private static Instant startOfDay(LocalDate day) {
		return day.atStartOfDay(TipReportService.REPORT_ZONE).toInstant();
	}
}
