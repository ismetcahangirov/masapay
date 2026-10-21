package az.masapay.repository;

import static org.assertj.core.api.Assertions.assertThat;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs every Flyway migration against a real PostgreSQL 16 and checks that the entities match the
 * schema (ddl-auto=validate) and that the custom queries work (#52). Skipped when Docker is absent.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PersistenceIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private RestaurantRepository restaurantRepository;
	@Autowired
	private RestaurantTableRepository tableRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private TransactionRepository transactionRepository;

	@Test
	void migrationsApplyAndPaymentQueriesWork() {
		Restaurant restaurant = new Restaurant();
		restaurant.setName("Dolma");
		restaurant = restaurantRepository.save(restaurant);

		RestaurantTable table = new RestaurantTable();
		table.setRestaurant(restaurant);
		table.setLabel("Masa 1");
		table = tableRepository.save(table);

		Order order = new Order();
		order.setRestaurant(restaurant);
		order.setTable(table);
		order.setTotalAmount(new BigDecimal("25.00"));
		order = orderRepository.save(order);

		Transaction transaction = new Transaction();
		transaction.setOrder(order);
		transaction.setRestaurant(restaurant);
		transaction.setAmount(new BigDecimal("25.00"));
		transaction.setTotalAmount(new BigDecimal("25.00"));
		transaction.setPayriffOrderId("ord-1");
		transaction = transactionRepository.saveAndFlush(transaction);

		assertThat(orderRepository.findOpenOrderForUpdate(table.getId())).contains(order);
		assertThat(transactionRepository.findByIdForUpdate(transaction.getId())).isPresent();
		assertThat(transactionRepository.findByPayriffOrderId("ord-1")).isPresent();
		assertThat(transactionRepository.findByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING))
				.hasSize(1);
	}
}
