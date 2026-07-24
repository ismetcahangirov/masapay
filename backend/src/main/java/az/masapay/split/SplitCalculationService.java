package az.masapay.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Central calculator for the three bill-splitting modes (EPIC 5). Pure and
 * stateless: it works on amounts, not entities, so the split-mode controllers
 * (#24 full, #25 item, #26 equal) load the order and delegate the arithmetic here.
 * <p>
 * Money is handled in integer cents internally so distributed shares always sum
 * back to the exact total (no rounding drift). Tips are added separately in #30.
 */
@Service
public class SplitCalculationService {

	/**
	 * FULL ("Tam Hesabı Ödə"): the outstanding amount to settle the whole bill.
	 *
	 * @return {@code max(0, orderTotal - alreadyPaid)} at scale 2
	 */
	public BigDecimal calculateFullPayment(BigDecimal orderTotal, BigDecimal alreadyPaid) {
		requireNonNegative(orderTotal, "orderTotal");
		requireNonNegative(alreadyPaid, "alreadyPaid");
		return orderTotal.subtract(alreadyPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * EQUAL ("Bərabər Böl"): the bill divided into {@code parts} shares that sum
	 * exactly to the total. Any leftover cents are spread across the first shares.
	 */
	public List<BigDecimal> splitEqually(BigDecimal orderTotal, int parts) {
		requireNonNegative(orderTotal, "orderTotal");
		if (parts < 1) {
			throw new IllegalArgumentException("parts must be at least 1");
		}
		long totalCents = toCents(orderTotal);
		long base = totalCents / parts;
		long remainder = totalCents % parts;

		List<BigDecimal> shares = new ArrayList<>(parts);
		for (int i = 0; i < parts; i++) {
			long cents = base + (i < remainder ? 1 : 0);
			shares.add(fromCents(cents));
		}
		return shares;
	}

	/**
	 * EQUAL for a payer covering {@code partsToPay} of {@code totalParts} shares
	 * (e.g. one guest paying for two seats). Uses the exact share distribution.
	 */
	public BigDecimal calculateEqualShare(BigDecimal orderTotal, int totalParts, int partsToPay) {
		if (partsToPay < 0 || partsToPay > totalParts) {
			throw new IllegalArgumentException("partsToPay must be between 0 and totalParts");
		}
		List<BigDecimal> shares = splitEqually(orderTotal, totalParts);
		return shares.subList(0, partsToPay).stream()
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * ITEM ("Öz Yediyini Ödə"): the sum of the line-item amounts a payer selected.
	 */
	public BigDecimal calculateItemPayment(List<BigDecimal> selectedItemAmounts) {
		if (selectedItemAmounts == null) {
			throw new IllegalArgumentException("selectedItemAmounts must not be null");
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (BigDecimal amount : selectedItemAmounts) {
			requireNonNegative(amount, "item amount");
			sum = sum.add(amount);
		}
		return sum.setScale(2, RoundingMode.HALF_UP);
	}

	private long toCents(BigDecimal amount) {
		return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	private BigDecimal fromCents(long cents) {
		return BigDecimal.valueOf(cents, 2);
	}

	private void requireNonNegative(BigDecimal value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " must not be null");
		}
		if (value.signum() < 0) {
			throw new IllegalArgumentException(name + " must not be negative");
		}
	}
}
