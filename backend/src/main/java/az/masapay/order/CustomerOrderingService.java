package az.masapay.order;

import az.masapay.domain.MenuItem;
import az.masapay.domain.RestaurantTable;
import az.masapay.menu.MenuService;
import az.masapay.order.dto.PlaceOrderRequest;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.restaurant.RestaurantGate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * A customer at the table confirms their basket and it goes onto the table's bill (#140). The
 * basket itself lives on the phone until then, so dropping a dish before confirming needs no
 * server call; once confirmed, a line belongs to the bill and customers have no way to remove it.
 * Every dish must be on today's menu, and its price comes from the menu, never from the phone.
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomerOrderingService {

	private final RestaurantTableRepository tableRepository;
	private final MenuItemRepository menuItemRepository;
	private final OpenBillWriter billWriter;
	private final CustomerOrderService customerOrderService;
	private final Clock clock;

	@Autowired
	public CustomerOrderingService(RestaurantTableRepository tableRepository, MenuItemRepository menuItemRepository,
			OpenBillWriter billWriter, CustomerOrderService customerOrderService) {
		this(tableRepository, menuItemRepository, billWriter, customerOrderService, Clock.systemUTC());
	}

	CustomerOrderingService(RestaurantTableRepository tableRepository, MenuItemRepository menuItemRepository,
			OpenBillWriter billWriter, CustomerOrderService customerOrderService, Clock clock) {
		this.tableRepository = tableRepository;
		this.menuItemRepository = menuItemRepository;
		this.billWriter = billWriter;
		this.customerOrderService = customerOrderService;
		this.clock = clock;
	}

	@Transactional
	public TableOrderResponse placeOrder(UUID qrToken, PlaceOrderRequest request) {
		RestaurantTable table = tableRepository.findByQrToken(qrToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
		// A restaurant the super admin switched off takes no orders (#147).
		RestaurantGate.checkActive(table.getRestaurant());

		// The same dish twice in one basket becomes one line with the quantities added up.
		Map<UUID, Integer> quantities = request.items().stream()
			.collect(Collectors.toMap(PlaceOrderRequest.Line::menuItemId, PlaceOrderRequest.Line::quantity,
				Integer::sum, LinkedHashMap::new));
		Map<UUID, MenuItem> dishes = menuItemRepository.findAllById(quantities.keySet()).stream()
			.collect(Collectors.toMap(MenuItem::getId, Function.identity()));

		LocalDate today = LocalDate.now(clock.withZone(MenuService.MENU_ZONE));
		UUID restaurantId = table.getRestaurant().getId();
		List<OpenBillWriter.NewLine> lines = quantities.entrySet().stream().map(entry -> {
			MenuItem dish = dishes.get(entry.getKey());
			if (dish == null || !dish.getRestaurant().getId().equals(restaurantId) || !dish.isAvailableOn(today)) {
				// Switched off or deleted while the customer was choosing: the phone reloads the menu.
				throw new ResponseStatusException(HttpStatus.CONFLICT, "A dish is no longer available");
			}
			return new OpenBillWriter.NewLine(dish.getName(), BigDecimal.valueOf(entry.getValue()), dish.getPrice(),
				dish, null);
		}).toList();

		billWriter.append(table, lines);
		return customerOrderService.getCurrentOrder(qrToken);
	}
}
