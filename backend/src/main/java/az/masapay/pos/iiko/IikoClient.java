package az.masapay.pos.iiko;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Thin HTTP client for the iiko Cloud API (#40). Knows URLs and JSON shapes, no business rules. */
@Component
public class IikoClient {

	/** iiko tokens live 60 minutes; renew 10 minutes early so a request never carries a dead one. */
	private static final Duration TOKEN_TTL = Duration.ofMinutes(50);

	private final RestClient restClient;
	private final Map<String, CachedToken> tokens = new ConcurrentHashMap<>();

	public IikoClient(RestClient.Builder builder, @Value("${masapay.iiko.base-url}") String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
	}

	/** Exchanges the restaurant's API login for a short-lived access token. */
	public String accessToken(String apiLogin) {
		return restClient.post()
				.uri("/api/1/access_token")
				.body(new IikoTokenRequest(apiLogin))
				.retrieve()
				.body(IikoTokenResponse.class)
				.token();
	}

	/** Reads one order from iiko by its id. */
	public IikoOrder fetchOrder(String apiLogin, UUID organizationId, UUID orderId) {
		IikoOrdersResponse response = restClient.post()
				.uri("/api/1/order/by_id")
				.headers(headers -> headers.setBearerAuth(token(apiLogin)))
				.body(new IikoOrdersRequest(List.of(organizationId), List.of(orderId)))
				.retrieve()
				.body(IikoOrdersResponse.class);
		if (response == null || response.orders().isEmpty()) {
			throw new IllegalStateException("iiko order " + orderId + " not found");
		}
		return response.orders().get(0).order();
	}

	/** Books a payment masapay already took onto the iiko order. */
	public void addPayment(String apiLogin, UUID organizationId, UUID orderId, UUID paymentTypeId, BigDecimal amount) {
		IikoPayment payment = new IikoPayment("External", amount, paymentTypeId, true);
		restClient.post()
				.uri("/api/1/order/add_payments")
				.headers(headers -> headers.setBearerAuth(token(apiLogin)))
				.body(new IikoAddPaymentsRequest(organizationId, orderId, List.of(payment)))
				.retrieve()
				.toBodilessEntity();
	}

	/** Closes the iiko order once it is fully paid, which frees the table in the POS. */
	public void closeOrder(String apiLogin, UUID organizationId, UUID orderId) {
		restClient.post()
				.uri("/api/1/order/close")
				.headers(headers -> headers.setBearerAuth(token(apiLogin)))
				.body(new IikoCloseOrderRequest(organizationId, orderId))
				.retrieve()
				.toBodilessEntity();
	}

	/** A token for this login, reused from the cache until it is about to expire. */
	private String token(String apiLogin) {
		CachedToken cached = tokens.get(apiLogin);
		if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
			return cached.token();
		}
		String fresh = accessToken(apiLogin);
		tokens.put(apiLogin, new CachedToken(fresh, Instant.now().plus(TOKEN_TTL)));
		return fresh;
	}

	private record CachedToken(String token, Instant expiresAt) {}
}
