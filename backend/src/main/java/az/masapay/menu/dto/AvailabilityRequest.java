package az.masapay.menu.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * How a dish appears on the daily menu (#139). {@code from} and {@code until} are read only
 * for {@link Mode#DATES}; at least one of them is required there.
 */
public record AvailabilityRequest(@NotNull Mode mode, LocalDate from, LocalDate until) {

	public enum Mode {
		/** Always on the menu. */
		ALWAYS,
		/** Off until switched back on. */
		OFF,
		/** Off for today only (Asia/Baku); back on tomorrow by itself. */
		OFF_TODAY,
		/** Only between {@code from} and {@code until}, both included. */
		DATES
	}
}
