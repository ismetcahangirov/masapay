package az.masapay.payment.payriff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

	@Test
	void refundsAnOrderAndFailsOnANonSuccessCode() {
		server.expect(requestTo("https://payriff.test/api/v3/refund"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "app-key-1"))
				.andExpect(jsonPath("$.orderId").value("ord-1"))
				.andExpect(jsonPath("$.amount").value(27.5))
				.andRespond(withSuccess("""
						{"code": "00000", "message": "OK", "payload": null}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://payriff.test/api/v3/refund"))
				.andRespond(withSuccess("""
						{"code": "15400", "message": "Already refunded", "payload": null}
						""", MediaType.APPLICATION_JSON));

		PayriffRefundRequest refund = new PayriffRefundRequest("ord-1", new BigDecimal("27.50"), "wrong dish");
		client.refund(refund);

		assertThatThrownBy(() -> client.refund(refund))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("15400");
		server.verify();
	}

	@Test
	void sendsTheRrnAndReadsPayriffsReasonFromAnHttpError() {
		server.expect(requestTo("https://payriff.test/api/v3/orders"))
				.andExpect(header("X-REQUEST-RRN", "6f1c2c1e-0000-4000-8000-000000000001"))
				.andRespond(withSuccess("""
						{"code": "00000", "message": "OK", "payload": {"orderId": "ord-1", "sessionId": "s",
						 "paymentUrl": "https://pay/ord-1", "transactionId": 1}}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://payriff.test/api/v3/refund"))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("""
						{"code": "01000", "message": "Refund time expired.\\n Support ticket: 840003",
						 "route": "dashboard/main", "responseDetails": null}
						"""));

		client.createOrder(request, "6f1c2c1e-0000-4000-8000-000000000001");

		PayriffRefundRequest refund = new PayriffRefundRequest("ord-1", new BigDecimal("27.50"), null);
		assertThatThrownBy(() -> client.refund(refund))
				.isInstanceOfSatisfying(PayriffException.class, e -> {
					assertThat(e.refused()).isTrue();
					assertThat(e.httpStatus()).isEqualTo(400);
					assertThat(e.payriffMessage()).startsWith("Refund time expired.");
				});
		server.verify();
	}

	@Test
	void anOutageIsNotARefusal() {
		server.expect(requestTo("https://payriff.test/api/v3/orders"))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML)
						.body("<html>Bad Gateway</html>"));

		assertThatThrownBy(() -> client.createOrder(request))
				.isInstanceOfSatisfying(PayriffException.class, e -> {
					assertThat(e.refused()).isFalse();
					assertThat(e.httpStatus()).isEqualTo(502);
				});
	}

	@Test
	void readsAnOrdersStatus() {
		server.expect(requestTo("https://payriff.test/api/v3/orders/ord-1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{"code": "00000", "message": "OK", "payload": {"orderId": "ord-1", "paymentStatus": "APPROVED",
						 "amount": 27.50}}
						""", MediaType.APPLICATION_JSON));

		assertThat(client.getOrder("ord-1")).isEqualTo(new PayriffOrderInfo("ord-1", "APPROVED"));
	}
}
