package az.masapay.payment.payriff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.masapay.domain.Transaction;
import az.masapay.domain.enums.TransactionStatus;
import az.masapay.repository.TransactionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PayriffReturnServiceTest {

	private final PayriffSignature signature = new PayriffSignature("test-webhook-secret-0123456789-abcdefghij");
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final PayriffClient client = mock(PayriffClient.class);
	private final PayriffWebhookService webhookService = mock(PayriffWebhookService.class);
	private final PayriffService payriffService = mock(PayriffService.class);
	private final PayriffReturnService service = new PayriffReturnService(signature, transactionRepository, client,
			webhookService, payriffService);

	private final Transaction transaction = new Transaction();
	private String txId;

	@BeforeEach
	void setUp() {
		transaction.setId(UUID.randomUUID());
		transaction.setStatus(TransactionStatus.PENDING);
		transaction.setPayriffOrderId("ord-1");
		txId = transaction.getId().toString();
		when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
		when(payriffService.resultPage(txId)).thenReturn("https://masapay.test/payment/result?tx=" + txId);
	}

	@Test
	void theGuestIsSentToTheResultPageAndThePaymentIsCheckedWithPayriff() {
		when(client.getOrder("ord-1")).thenReturn(new PayriffOrderInfo("ord-1", "APPROVED"));

		String next = service.guestReturned(txId, signature.sign(txId));

		assertThat(next).isEqualTo("https://masapay.test/payment/result?tx=" + txId);
		// Settled now, even if Payriff's posted callback is late or lost.
		verify(webhookService).apply(transaction.getId(), "ord-1", "APPROVED");
	}

	@Test
	void payriffNotAnsweringStillSendsTheGuestOn() {
		when(client.getOrder("ord-1")).thenThrow(new PayriffException("getOrder", 0, null, "timeout"));

		assertThat(service.guestReturned(txId, signature.sign(txId))).contains("/payment/result");
	}

	@Test
	void aSettledPaymentIsNotAskedAgainAndABadSignatureIsRefused() {
		transaction.setStatus(TransactionStatus.SUCCESS);
		service.guestReturned(txId, signature.sign(txId));
		verify(client, never()).getOrder(any());

		assertThatThrownBy(() -> service.guestReturned(txId, "forged"))
				.isInstanceOf(ResponseStatusException.class);
	}
}
