package az.masapay.payment.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.domain.Restaurant;
import az.masapay.domain.Transaction;
import az.masapay.domain.User;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.domain.enums.UserRole;
import az.masapay.payment.payriff.PayriffClient;
import az.masapay.payment.payriff.PayriffRefundRequest;
import az.masapay.repository.TransactionRepository;
import az.masapay.repository.UserRepository;
import az.masapay.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RefundServiceTest {

	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final PayriffClient payriffClient = mock(PayriffClient.class);
	private final RefundService service = new RefundService(transactionRepository, userRepository, payriffClient);

	private final AuthenticatedUser superAdmin = new AuthenticatedUser(UUID.randomUUID(), "a@x.az", UserRole.SUPER_ADMIN);
	private final Restaurant restaurant = new Restaurant();
	private final Transaction transaction = new Transaction();

	@BeforeEach
	void setUp() {
		restaurant.setId(UUID.randomUUID());
		transaction.setId(UUID.randomUUID());
		transaction.setRestaurant(restaurant);
		transaction.setStatus(TransactionStatus.SUCCESS);
		transaction.setTotalAmount(new BigDecimal("27.50"));
		transaction.setPayriffOrderId("ord-1");
		when(transactionRepository.findByIdForUpdate(transaction.getId())).thenReturn(Optional.of(transaction));
	}


	@Test
	void refundsTheWholePaymentAndMarksItRefunded() {
		RefundResult result = service.refund(transaction.getId(), superAdmin, "wrong dish");

		verify(payriffClient).refund(new PayriffRefundRequest("ord-1", new BigDecimal("27.50"), "wrong dish"));
		assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
		assertThat(transaction.getRefundedAmount()).isEqualByComparingTo("27.50");
		assertThat(result.status()).isEqualTo("REFUNDED");
		verify(transactionRepository).save(transaction);
	}

	@Test
	void aSecondRefundDoesNotCallPayriffAgain() {
		transaction.setStatus(TransactionStatus.REFUNDED);

		RefundResult result = service.refund(transaction.getId(), superAdmin, null);

		assertThat(result.status()).isEqualTo("REFUNDED");
		verifyNoInteractions(payriffClient);
	}

	@Test
	void onlyASuccessfulPayriffPaymentCanBeRefunded() {
		transaction.setStatus(TransactionStatus.PENDING);
		assertThatThrownBy(() -> service.refund(transaction.getId(), superAdmin, null))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

		transaction.setStatus(TransactionStatus.SUCCESS);
		transaction.setPayriffOrderId(null);
		assertThatThrownBy(() -> service.refund(transaction.getId(), superAdmin, null))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verifyNoInteractions(payriffClient);
	}

	@Test
	void aManagerCannotRefundAnotherRestaurantsPayment() {
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		Restaurant other = new Restaurant();
		other.setId(UUID.randomUUID());
		User user = new User();
		user.setRestaurant(other);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.refund(transaction.getId(), manager, null))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(payriffClient, never()).refund(any());
	}

	@Test
	void aManagerCanRefundTheirOwnRestaurantsPayment() {
		AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "m@x.az", UserRole.RESTO_MANAGER);
		User user = new User();
		user.setRestaurant(restaurant);
		when(userRepository.findById(manager.id())).thenReturn(Optional.of(user));

		service.refund(transaction.getId(), manager, null);

		assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
	}
}
