package az.masapay.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SplitCalculationServiceTest {

	private final SplitCalculationService service = new SplitCalculationService();

	@Nested
	class FullPayment {

		@Test
		void returnsFullTotalWhenNothingPaid() {
			assertThat(service.calculateFullPayment(new BigDecimal("50.00"), BigDecimal.ZERO))
				.isEqualByComparingTo("50.00");
		}

		@Test
		void returnsOutstandingAfterPartialPayment() {
			assertThat(service.calculateFullPayment(new BigDecimal("50.00"), new BigDecimal("20.00")))
				.isEqualByComparingTo("30.00");
		}

		@Test
		void neverNegativeWhenOverpaid() {
			assertThat(service.calculateFullPayment(new BigDecimal("50.00"), new BigDecimal("60.00")))
				.isEqualByComparingTo("0.00");
		}

		@Test
		void rejectsNegativeInputs() {
			assertThatThrownBy(() -> service.calculateFullPayment(new BigDecimal("-1"), BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> service.calculateFullPayment(BigDecimal.TEN, new BigDecimal("-1")))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> service.calculateFullPayment(null, BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class EqualSplit {

		@Test
		void dividesEvenlyWhenNoRemainder() {
			assertThat(service.splitEqually(new BigDecimal("12.00"), 3))
				.containsExactly(new BigDecimal("4.00"), new BigDecimal("4.00"), new BigDecimal("4.00"));
		}

		@Test
		void spreadsLeftoverCentsAcrossFirstShares() {
			List<BigDecimal> shares = service.splitEqually(new BigDecimal("10.00"), 3);
			assertThat(shares).containsExactly(new BigDecimal("3.34"), new BigDecimal("3.33"), new BigDecimal("3.33"));
		}

		@Test
		void sharesAlwaysSumBackToTotal() {
			List<BigDecimal> shares = service.splitEqually(new BigDecimal("100.00"), 7);
			BigDecimal sum = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
			assertThat(sum).isEqualByComparingTo("100.00");
			assertThat(shares).hasSize(7);
		}

		@Test
		void singlePartReturnsWholeTotal() {
			assertThat(service.splitEqually(new BigDecimal("9.99"), 1)).containsExactly(new BigDecimal("9.99"));
		}

		@Test
		void rejectsZeroOrNegativeParts() {
			assertThatThrownBy(() -> service.splitEqually(BigDecimal.TEN, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class EqualShareForPayer {

		@Test
		void sumsTheFirstPartsToPay() {
			// 10.00 / 3 -> [3.34, 3.33, 3.33]; paying 2 parts -> 6.67
			assertThat(service.calculateEqualShare(new BigDecimal("10.00"), 3, 2)).isEqualByComparingTo("6.67");
		}

		@Test
		void payingAllPartsEqualsTotal() {
			assertThat(service.calculateEqualShare(new BigDecimal("10.00"), 3, 3)).isEqualByComparingTo("10.00");
		}

		@Test
		void payingZeroPartsIsZero() {
			assertThat(service.calculateEqualShare(new BigDecimal("10.00"), 3, 0)).isEqualByComparingTo("0.00");
		}

		@Test
		void rejectsPartsToPayOutOfRange() {
			assertThatThrownBy(() -> service.calculateEqualShare(BigDecimal.TEN, 3, 4))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> service.calculateEqualShare(BigDecimal.TEN, 3, -1))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class ItemPayment {

		@Test
		void sumsSelectedItemAmounts() {
			assertThat(service.calculateItemPayment(List.of(new BigDecimal("3.50"), new BigDecimal("4.50"))))
				.isEqualByComparingTo("8.00");
		}

		@Test
		void emptySelectionIsZero() {
			assertThat(service.calculateItemPayment(List.of())).isEqualByComparingTo("0.00");
		}

		@Test
		void rejectsNullSelectionAndNegativeAmounts() {
			assertThatThrownBy(() -> service.calculateItemPayment(null))
				.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> service.calculateItemPayment(List.of(new BigDecimal("-1.00"))))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}
}
