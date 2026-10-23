package az.masapay.order;

import az.masapay.domain.MenuItem;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.User;
import az.masapay.domain.enums.UserRole;
import az.masapay.menu.MenuService;
import az.masapay.order.dto.AddMenuItemRequest;
import az.masapay.order.dto.AddOrderItemRequest;
import az.masapay.order.dto.TableOrderResponse;
import az.masapay.order.dto.TablePaymentView;
import az.masapay.repository.MenuItemRepository;
import az.masapay.repository.RestaurantTableRepository;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Staff-side changes to a table's bill (#21). Adding an item opens the bill if the table has none,
 * updates the total and pushes the fresh bill to every customer screen on that table. Every line
 * remembers who added it, and staff can add a dish from the menu like a customer would, so it
 * goes to the kitchen (#144).
 */
@Service
@ConditionalOnProperty(prefix = "masapay.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaffOrderService {

	private final RestaurantTableRepository tableRepository;
	private final UserRepository userRepository;
	private final MenuItemRepository menuItemRepository;
	private final TransactionRepository transactionRepository;
	private final CustomerOrderService customerOrderService;
	private final OpenBillWriter billWriter;
	private final Clock clock;

	@Autowired
	public StaffOrderService(RestaurantTableRepository tableRepository, UserRepository userRepository,
			MenuItemRepository menuItemRepository, TransactionRepository transactionRepository,
			CustomerOrderService customerOrderService, OpenBillWriter billWriter) {
		this(tableRepository, userRepository, menuItemRepository, transactionRepository, customerOrderService,
			billWriter, Clock.systemUTC());
	}

	StaffOrderService(RestaurantTableRepository tableRepository, UserRepository userRepository,
			MenuItemRepository menuItemRepository, TransactionRepository transactionRepository,
			CustomerOrderService customerOrderService, OpenBillWriter billWriter, Clock clock) {
		this.tableRepository = tableRepository;
		this.userRepository = userRepository;
		this.menuItemRepository = menuItemRepository;
		this.transactionRepository = transactionRepository;
		this.customerOrderService = customerOrderService;
		this.billWriter = billWriter;
		this.clock = clock;
	}

	@Transactional
	public TableOrderResponse addItem(UUID tableId, AddOrderItemRequest request, AuthenticatedUser staff) {
		RestaurantTable table = table(tableId);
		User user = checkSameRestaurant(staff, table);

		billWriter.append(table,
			List.of(new OpenBillWriter.NewLine(request.name(), request.quantity(), request.unitPrice(), null, user)));
		return customerOrderService.getCurrentOrder(table.getQrToken());
	}

	/** A dish from today's menu, at its menu price; it goes to the kitchen like a customer order. */
	@Transactional
	public TableOrderResponse addMenuItem(UUID tableId, AddMenuItemRequest request, AuthenticatedUser staff) {
		RestaurantTable table = table(tableId);
		User user = checkSameRestaurant(staff, table);
		LocalDate today = LocalDate.now(clock.withZone(MenuService.MENU_ZONE));
		MenuItem dish = menuItemRepository.findById(request.menuItemId())
			.filter(item -> item.getRestaurant().getId().equals(table.getRestaurant().getId()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found"));
		if (!dish.isAvailableOn(today)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The dish is not on today's menu");
		}

		billWriter.append(table, List.of(new OpenBillWriter.NewLine(dish.getName(),
			BigDecimal.valueOf(request.quantity()), dish.getPrice(), dish, user)));
		return customerOrderService.getCurrentOrder(table.getQrToken());
	}

	/**
	 * A waiter takes the table (#182): from now on they serve it and its tips are theirs. Another
	 * waiter may take it over. Only an open bill can be taken.
	 */
	@Transactional
	public TableOrderResponse claim(UUID tableId, AuthenticatedUser staff) {
		RestaurantTable table = table(tableId);
		User user = checkSameRestaurant(staff, table);
		if (staff.role() != UserRole.WAITER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a waiter takes a table");
		}
		billWriter.assignWaiter(table, user);
		return customerOrderService.getCurrentOrder(table.getQrToken());
	}

	/** Payments on the table's current bill, newest first (#144); empty when it has no bill. */
	@Transactional(readOnly = true)
	public List<TablePaymentView> payments(UUID tableId, AuthenticatedUser staff) {
		RestaurantTable table = table(tableId);
		checkSameRestaurant(staff, table);
		TableOrderResponse snapshot = customerOrderService.getCurrentOrder(table.getQrToken());
		if (snapshot.order() == null) {
			return List.of();
		}
		return transactionRepository.findByOrderId(snapshot.order().orderId()).stream()
			.sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
			.map(TablePaymentView::of)
			.toList();
	}

	private RestaurantTable table(UUID tableId) {
		return tableRepository.findById(tableId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
	}

	/**
	 * Staff may only change bills of their own restaurant; a super admin any. Returns the staff
	 * member, who is recorded on the lines they add.
	 */
	private User checkSameRestaurant(AuthenticatedUser staff, RestaurantTable table) {
		User user = userRepository.findById(staff.id())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
		if (staff.role() == UserRole.SUPER_ADMIN) {
			return user;
		}
		if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(table.getRestaurant().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your restaurant");
		}
		return user;
	}
}
