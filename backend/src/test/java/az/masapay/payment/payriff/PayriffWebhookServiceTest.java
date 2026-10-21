package az.masapay.payment.payriff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.domain.Order;
import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.payment.PaymentSettlementService;
import az.masapay.repository.TransactionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PayriffWebhookServiceTest {

	private final PayriffSignature signature = new PayriffSignature("test-secret-0123456789-0123456789");
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final PaymentSettlementService settlementService = mock(PaymentSettlementService.class);
	private final PayriffWebhookService service =
			new PayriffWebhookService(signature, transactionRepository, settlementService);

	private final Transaction transaction = new Transaction();
	private final Order order = new Order();
	private String txId;
	private String sig;

	@BeforeEach
	void setUp() {
		transaction.setId(UUID.randomUUID());
		transaction.setOrder(order);
		transaction.setPayriffOrderId("ord-1");
		txId = transaction.getId().toString();
		sig = signature.sign(txId);
		when(transactionRepository.findByIdForUpdate(transaction.getId())).thenReturn(Optional.of(transaction));
	}

	private static PayriffCallback callback(String orderId, String status) {
		return new PayriffCallback(new PayriffCallback.Payload(orderId, status));
	}

	@Test
	void rejectsAWrongSignatureWith403BeforeTouchingTheDatabase() {
		assertThatThrownBy(() -> service.handle(txId, "forged", callback("ord-1", "APPROVED")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
		verifyNoInteractions(transactionRepository, settlementService);
	}

	@Test
	void approvedAppliesThePendingSplit() {
		service.handle(txId, sig, callback("ord-1", "APPROVED"));

		verify(settlementService).settlePending(transaction);
	}

	@Test
	void aRepeatedCallbackChangesNothing() {
		transaction.setStatus(TransactionStatus.SUCCESS);

		service.handle(txId, sig, callback("ord-1", "APPROVED"));

		verify(transactionRepository, never()).save(any());
		verifyNoInteractions(settlementService);
	}

	@Test
	void declinedMarksTheTransactionFailed() {
		service.handle(txId, sig, callback("ord-1", "DECLINED"));

		assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
		verifyNoInteractions(settlementService);
	}

	@Test
	void aNonFinalStatusKeepsTheTransactionPending() {
		service.handle(txId, sig, callback("ord-1", "IN_REVIEW"));

		assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void rejectsACallbackForAnotherPayriffOrder() {
		assertThatThrownBy(() -> service.handle(txId, sig, callback("ord-999", "APPROVED")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verifyNoInteractions(settlementService);
	}
}
