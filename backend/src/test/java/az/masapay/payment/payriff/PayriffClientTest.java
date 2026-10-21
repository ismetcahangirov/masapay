package az.masapay.payment.payriff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PayriffClientTest {

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final PayriffClient client = new PayriffClient(builder, "https://payriff.test", "app-key-1");

	private final PayriffCreateOrderRequest request = new PayriffCreateOrderRequest(new BigDecimal("27.50"), "AZN",
			"PURCHASE", "AZ", "masapay payment tx-1", "https://masapay.test/webhook?tx=tx-1&sig=abc",
			"https://masapay.test/result");

	@Test
	void createsAnOrderWithTheAppKeyAndReturnsThePaymentPage() {
		server.expect(requestTo("https://payriff.test/api/v3/orders"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "app-key-1"))
				.andExpect(jsonPath("$.amount").value(27.5))
				.andExpect(jsonPath("$.currency").value("AZN"))
				.andExpect(jsonPath("$.operation").value("PURCHASE"))
				.andExpect(jsonPath("$.callbackUrl").value("https://masapay.test/webhook?tx=tx-1&sig=abc"))
				.andRespond(withSuccess("""
						{"code": "00000", "message": "OK", "payload": {"orderId": "ord-1", "sessionId": "ses-1",
						 "paymentUrl": "https://pay.payriff.com/ord-1", "transactionId": 777}}
						""", MediaType.APPLICATION_JSON));

		PayriffOrder order = client.createOrder(request);

		assertThat(order).isEqualTo(new PayriffOrder("ord-1", "ses-1", "https://pay.payriff.com/ord-1", 777L));
		server.verify();
	}

	@Test
	void treatsANonSuccessCodeAsAFailureEvenWithHttp200() {
		server.expect(requestTo("https://payriff.test/api/v3/orders"))
				.andRespond(withSuccess("""
						{"code": "01000", "message": "Invalid app key", "payload": null}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.createOrder(request))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("01000");
	}
}
