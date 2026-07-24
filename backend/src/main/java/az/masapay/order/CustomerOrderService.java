package az.masapay.order;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.enums.OrderStatus;
import az.masapay.order.dto.OrderView;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.repository.OrderItemRepository;
import az.masapay.repository.OrderRepository;
import az.masapay.repository.RestaurantTableRepository;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the live order for a table from its QR token, for the customer PWA. */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerOrderService {

	private final RestaurantTableRepository tableRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;

	public CustomerOrderService(RestaurantTableRepository tableRepository, OrderRepository orderRepository,
			OrderItemRepository orderItemRepository) {
		this.tableRepository = tableRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
	}

	@Transactional(readOnly = true)
	public TableOrderResponse getCurrentOrder(UUID qrToken) {
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		Restaurant restaurant = table.getRestaurant();

		OrderView orderView = orderRepository
			.findFirstByTableIdAndStatusOrderByOpenedAtDesc(table.getId(), OrderStatus.OPEN)
			.map(this::toOrderView)
			.orElse(null);

		return new TableOrderResponse(
			table.getId(), table.getLabel(), restaurant.getId(), restaurant.getName(),
			restaurant.getCurrency(), orderView);
	}

	private OrderView toOrderView(Order order) {
		return OrderView.of(order, orderItemRepository.findByOrderId(order.getId()));
	}
}
