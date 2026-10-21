package az.masapay.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.Restaurant;
import az.masapay.domain.RestaurantTable;
import az.masapay.domain.Transaction;
import az.masapay.domain.User;
import az.masapay.domain.enums.SplitType;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import az.masapay.transaction.dto.TransactionPage;
import az.masapay.transaction.dto.TransactionRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransactionSearchServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private UserRepository userRepository;

	private TransactionSearchService service;

	private final UUID restaurantId = UUID.randomUUID();
	private final AuthenticatedUser superAdmin =
		new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", UserRole.SUPER_ADMIN);
	private final AuthenticatedUser manager =
		new AuthenticatedUser(UUID.randomUUID(), "manager@example.com", UserRole.RESTO_MANAGER);

	@BeforeEach
	void setUp() {
		service = new TransactionSearchService(transactionRepository, userRepository);
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<Pageable> stubPage(List<Transaction> content) {
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		when(transactionRepository.findAll(any(Specification.class), pageable.capture()))
			.thenAnswer(call -> new PageImpl<>(content, call.getArgument(1), content.size()));
		return pageable;
	}

	private void managerOf(UUID ownRestaurantId) {
		Restaurant own = new Restaurant();
		own.setId(ownRestaurantId);
		User user = new User();
		user.setRestaurant(own);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));
	}

	private static Transaction transaction(TransactionStatus status, String payriffOrderId) {
		RestaurantTable table = new RestaurantTable();
		table.setLabel("Masa 7");
		Order order = new Order();
		order.setTable(table);
		Transaction transaction = new Transaction();
		transaction.setId(UUID.randomUUID());
		transaction.setOrder(order);
		transaction.setStatus(status);
		transaction.setSplitType(SplitType.EQUAL);
		transaction.setAmount(new BigDecimal("40.00"));
		transaction.setTipAmount(new BigDecimal("4.00"));
		transaction.setTotalAmount(new BigDecimal("44.00"));
		transaction.setPayriffOrderId(payriffOrderId);
		return transaction;
	}

	@Test
	void managerOfAnotherRestaurantGets403() {
		managerOf(UUID.randomUUID());

		assertThatThrownBy(() -> service.search(manager, restaurantId, null, null, null, null, null, 0, 20))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void waiterGets403() {
		AuthenticatedUser waiter = new AuthenticatedUser(UUID.randomUUID(), "w@example.com", UserRole.WAITER);

		assertThatThrownBy(() -> service.search(waiter, restaurantId, null, null, null, null, null, 0, 20))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void managerReadsOwnRestaurant() {
		managerOf(restaurantId);
		stubPage(List.of());

		TransactionPage page = service.search(manager, restaurantId, null, null, null, null, null, 0, 20);

		assertThat(page.items()).isEmpty();
		assertThat(page.totalElements()).isZero();
	}

	@Test
	void onlySuccessfulPayriffPaymentsAreRefundable() {
		stubPage(List.of(
			transaction(TransactionStatus.SUCCESS, "PRF-1"),
			transaction(TransactionStatus.SUCCESS, null),
			transaction(TransactionStatus.REFUNDED, "PRF-2"),
			transaction(TransactionStatus.PENDING, "PRF-3")));

		TransactionPage page = service.search(superAdmin, restaurantId, null, null, null, null, null, 0, 20);

		assertThat(page.items()).extracting(TransactionRow::refundable).containsExactly(true, false, false, false);
		assertThat(page.items().get(0).tableLabel()).isEqualTo("Masa 7");
		assertThat(page.items().get(0).totalAmount()).isEqualByComparingTo("44.00");
	}

	@Test
	void sizeIsCappedAndSortedNewestFirst() {
		ArgumentCaptor<Pageable> pageable = stubPage(List.of());

		TransactionPage page = service.search(superAdmin, restaurantId, null, null, null, null, null, 2, 500);

		assertThat(pageable.getValue())
			.isEqualTo(PageRequest.of(2, 100, Sort.by(Sort.Direction.DESC, "createdAt")));
		assertThat(page.size()).isEqualTo(100);
		assertThat(page.page()).isEqualTo(2);
	}

	@Test
	void invalidPageAndSizeFallBack() {
		ArgumentCaptor<Pageable> pageable = stubPage(List.of());

		service.search(superAdmin, restaurantId, null, null, null, null, null, -1, 0);

		assertThat(pageable.getValue().getPageNumber()).isZero();
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
	}

	@Test
	void inclusiveBakuDaysBecomeHalfOpenRange() {
		TransactionFilter filter = TransactionSearchService.toFilter(restaurantId, TransactionStatus.SUCCESS,
			SplitType.FULL, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "  Masa 7 ");

		assertThat(filter.createdFrom()).isEqualTo(Instant.parse("2026-09-30T20:00:00Z"));
		assertThat(filter.createdTo()).isEqualTo(Instant.parse("2026-10-31T20:00:00Z"));
		assertThat(filter.query()).isEqualTo("Masa 7");
		assertThat(filter.status()).isEqualTo(TransactionStatus.SUCCESS);
		assertThat(filter.splitType()).isEqualTo(SplitType.FULL);
	}

	@Test
	void missingDatesAndBlankQueryMeanNoFilter() {
		TransactionFilter filter = TransactionSearchService.toFilter(restaurantId, null, null, null, null, "   ");

		assertThat(filter.createdFrom()).isNull();
		assertThat(filter.createdTo()).isNull();
		assertThat(filter.query()).isNull();
	}

	@Test
	void fromAfterToIs400() {
		assertThatThrownBy(() -> TransactionSearchService.toFilter(restaurantId, null, null,
			LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 4), null))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void likeWildcardsAreEscaped() {
		assertThat(TransactionSpecifications.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
	}

	@Test
	@SuppressWarnings("unchecked")
	void superAdminSkipsOwnershipLookup() {
		stubPage(List.of());

		service.search(superAdmin, restaurantId, null, null, null, null, null, 0, 20);

		verifyNoInteractions(userRepository);
		verify(transactionRepository).findAll(any(Specification.class), any(Pageable.class));
	}
}
