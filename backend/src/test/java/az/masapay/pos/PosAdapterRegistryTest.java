package az.masapay.pos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import az.masapay.domain.Restaurant;
import az.masapay.domain.enums.PosType;
import az.masapay.pos.iiko.IikoClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class PosAdapterRegistryTest {

	private final IikoPosAdapter iiko = new IikoPosAdapter(mock(IikoClient.class));
	private final MicrosPosAdapter micros = new MicrosPosAdapter();
	private final CustomPosAdapter custom = new CustomPosAdapter();

	@Test
	void picksTheAdapterMatchingTheRestaurantsPosType() {
		PosAdapterRegistry registry = new PosAdapterRegistry(List.of(iiko, micros, custom));
		Restaurant restaurant = new Restaurant();
		restaurant.setPosType(PosType.IIKO);

		assertThat(registry.forRestaurant(restaurant)).isSameAs(iiko);
	}

	@Test
	void everyPosTypeHasAnAdapter() {
		PosAdapterRegistry registry = new PosAdapterRegistry(List.of(iiko, micros, custom));

		for (PosType type : PosType.values()) {
			assertThat(registry.get(type).type()).isEqualTo(type);
		}
	}

	@Test
	void rejectsTwoAdaptersForTheSamePosType() {
		assertThatThrownBy(() -> new PosAdapterRegistry(List.of(iiko, new IikoPosAdapter(mock(IikoClient.class)))))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("IIKO");
	}

	@Test
	void failsClearlyWhenNoAdapterIsRegistered() {
		PosAdapterRegistry registry = new PosAdapterRegistry(List.of(iiko));

		assertThatThrownBy(() -> registry.get(PosType.MICROS))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("MICROS");
	}
}
