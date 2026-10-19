package az.masapay.pos.iiko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class IikoClientTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("9b1d0000-0000-4000-8000-000000000002");
	private static final UUID ORDER_ID = UUID.fromString("3f2a1c4e-0000-4000-8000-000000000001");

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final IikoClient client = new IikoClient(builder, "https://iiko.test");

	@Test
	void fetchesAnOrderWithABearerTokenAndReusesTheToken() {
		server.expect(once(), requestTo("https://iiko.test/api/1/access_token"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.apiLogin").value("api-login"))
				.andRespond(withSuccess("""
						{"token": "tok-1"}
						""", MediaType.APPLICATION_JSON));
		server.expect(times(2), requestTo("https://iiko.test/api/1/order/by_id"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-1"))
				.andExpect(jsonPath("$.orderIds[0]").value(ORDER_ID.toString()))
				.andRespond(withSuccess("""
						{"orders": [{"id": "x", "order": {"status": "Bill", "sum": 24.50, "items": []}}]}
						""", MediaType.APPLICATION_JSON));

		client.fetchOrder("api-login", ORGANIZATION_ID, ORDER_ID);
		IikoOrder order = client.fetchOrder("api-login", ORGANIZATION_ID, ORDER_ID);

		assertThat(order.status()).isEqualTo("Bill");
		assertThat(order.sum()).isEqualByComparingTo("24.50");
		server.verify();
	}

	@Test
	void failsWhenIikoDoesNotKnowTheOrder() {
		server.expect(requestTo("https://iiko.test/api/1/access_token"))
				.andRespond(withSuccess("""
						{"token": "tok-1"}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://iiko.test/api/1/order/by_id"))
				.andRespond(withSuccess("""
						{"orders": []}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.fetchOrder("api-login", ORGANIZATION_ID, ORDER_ID))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(ORDER_ID.toString());
	}

	@Test
	void booksAnExternalPaymentThenClosesTheOrder() {
		UUID paymentTypeId = UUID.fromString("5c2e0000-0000-4000-8000-000000000003");
		server.expect(requestTo("https://iiko.test/api/1/access_token"))
				.andRespond(withSuccess("""
						{"token": "tok-1"}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://iiko.test/api/1/order/add_payments"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-1"))
				.andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
				.andExpect(jsonPath("$.payments[0].paymentTypeKind").value("External"))
				.andExpect(jsonPath("$.payments[0].paymentTypeId").value(paymentTypeId.toString()))
				.andExpect(jsonPath("$.payments[0].sum").value(24.5))
				.andExpect(jsonPath("$.payments[0].isProcessedExternally").value(true))
				.andRespond(withSuccess());
		server.expect(requestTo("https://iiko.test/api/1/order/close"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-1"))
				.andExpect(jsonPath("$.organizationId").value(ORGANIZATION_ID.toString()))
				.andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
				.andRespond(withSuccess());

		client.addPayment("api-login", ORGANIZATION_ID, ORDER_ID, paymentTypeId, new BigDecimal("24.50"));
		client.closeOrder("api-login", ORGANIZATION_ID, ORDER_ID);

		server.verify();
	}
}
