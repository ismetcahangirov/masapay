package az.masapay.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Revenue and tips of one Asia/Baku calendar day, for the dashboard chart. */
public record DailyRevenue(LocalDate date, BigDecimal revenue, BigDecimal tips) {
}
