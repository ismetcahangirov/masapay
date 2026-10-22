package az.masapay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RestaurantTableTest {

	private RestaurantTable table(String restaurantPercent, String tablePercent) {
		Restaurant restaurant = new Restaurant();
		restaurant.setServiceChargePercent(restaurantPercent == null ? null : new BigDecimal(restaurantPercent));
		RestaurantTable table = new RestaurantTable();
		table.setRestaurant(restaurant);
		table.setServiceChargePercent(tablePercent == null ? null : new BigDecimal(tablePercent));
		return table;
	}

	@Test
	void aTableWithoutItsOwnPercentFollowsTheRestaurant() {
		assertThat(table("10.00", null).effectiveServiceChargePercent()).isEqualByComparingTo("10.00");
		assertThat(table(null, null).effectiveServiceChargePercent()).isNull();
	}

	@Test
	void aTablesOwnPercentWinsEvenWhereTheRestaurantChargesNone() {
		assertThat(table("10.00", "15.00").effectiveServiceChargePercent()).isEqualByComparingTo("15.00");
		assertThat(table(null, "5.00").effectiveServiceChargePercent()).isEqualByComparingTo("5.00");
	}

	@Test
	void zeroAtATableMeansNoServiceCharge() {
		assertThat(table("10.00", "0").effectiveServiceChargePercent()).isNull();
	}
}
