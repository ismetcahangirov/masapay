package az.masapay.kitchen.dto;

import az.masapay.domain.OrderItem;
import az.masapay.domain.enums.KitchenStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One ordered dish on the kitchen screen (#142): which table, what, how many, since when. */
public record KitchenLineView(UUID id, UUID tableId, String tableLabel, String name, BigDecimal quantity,
		KitchenStatus status, Instant orderedAt) {

	public static KitchenLineView of(OrderItem item) {
		return new KitchenLineView(item.getId(), item.getOrder().getTable().getId(),
			item.getOrder().getTable().getLabel(), item.getName(), item.getQuantity(), item.getKitchenStatus(),
			item.getCreatedAt());
	}
}
