package az.masapay.payment.payriff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.masapay.domain.Transaction;
import az.masapay.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

class PayriffServiceTest {

	private final PayriffClient client = mock(PayriffClient.class);
	private final PayriffSignature signature = new PayriffSignature("test-secret-0123456789-0123456789");
	private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
	private final PayriffService service = new PayriffService(client, signature, transactionRepository,
			"https://masapay.test/api/public/payriff/webhook", "https://masapay.test/payment/result", false,
			"https://masapay.test/payment/mock");

	@Test
	void opensACheckoutWithASignedCallbackAndStoresPayriffIds() {
		Transaction transaction = new Transaction();
		transaction.setId(UUID.randomUUID());
		transaction.setTotalAmount(new BigDecimal("27.50"));
		when(client.createOrder(any()))
				.thenReturn(new PayriffOrder("ord-1", "ses-1", "https://pay.payriff.com/ord-1", 777L));

		PayriffCheckout checkout = service.startCheckout(transaction);

		assertThat(checkout).isEqualTo(new PayriffCheckout("ord-1", "https://pay.payriff.com/ord-1"));
		assertThat(transaction.getPayriffOrderId()).isEqualTo("ord-1");
		assertThat(transaction.getPayriffSessionId()).isEqualTo("ses-1");
		assertThat(transaction.getPayriffPaymentId()).isEqualTo("777");
		verify(transactionRepository).save(transaction);

		ArgumentCaptor<PayriffCreateOrderRequest> sent = ArgumentCaptor.forClass(PayriffCreateOrderRequest.class);
		verify(client).createOrder(sent.capture());
		assertThat(sent.getValue().amount()).isEqualByComparingTo("27.50");
		assertThat(sent.getValue().currency()).isEqualTo("AZN");
		assertThat(sent.getValue().redirectUrl())
				.isEqualTo("https://masapay.test/payment/result?tx=" + transaction.getId());

		// The webhook (#35) will verify exactly these two query parameters.
		UriComponents callback = UriComponentsBuilder.fromUriString(sent.getValue().callbackUrl()).build();
		String tx = callback.getQueryParams().getFirst("tx");
		assertThat(tx).isEqualTo(transaction.getId().toString());
		assertThat(signature.matches(tx, callback.getQueryParams().getFirst("sig"))).isTrue();
	}

	@Test
	void mockModeSkipsPayriffAndPointsToTheMockPage() {
		PayriffService mockService = new PayriffService(client, signature, transactionRepository,
				"https://masapay.test/api/public/payriff/webhook", "https://masapay.test/payment/result", true,
				"https://masapay.test/payment/mock");
		Transaction transaction = new Transaction();
		transaction.setId(UUID.randomUUID());
		transaction.setTotalAmount(new BigDecimal("9.00"));

		PayriffCheckout checkout = mockService.startCheckout(transaction);

		verifyNoInteractions(client);
		assertThat(transaction.getPayriffOrderId()).isEqualTo("MOCK-" + transaction.getId());
		assertThat(checkout.paymentUrl()).startsWith("https://masapay.test/payment/mock?tx=" + transaction.getId())
				.contains("sig=" + signature.sign(transaction.getId().toString()));
	}
}
