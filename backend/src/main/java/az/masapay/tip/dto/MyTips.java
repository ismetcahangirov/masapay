package az.masapay.tip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A waiter's own tips over {@code [from, to]}, Baku days, newest first (#182). */
public record MyTips(LocalDate from, LocalDate to, String timeZone, String currency, long tipCount,
		BigDecimal tipTotal, List<MyTipLine> tips) {
}
