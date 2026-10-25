package az.masapay.payment.payriff;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin HTTP client for the Payriff API (#33). Knows URLs and JSON shapes, no business rules.
 * <p>
 * Every way a call can go wrong ends in a {@link PayriffException}: an HTTP error (Payriff puts
 * its result code and message in the body, e.g. a refund past 30 days is HTTP 400 {@code 01000}),
 * a success status with a non-success code, or no answer at all.
 */
@Component
public class PayriffClient {

	private static final ParameterizedTypeReference<PayriffResponse<PayriffOrder>> ORDER_RESPONSE =
			new ParameterizedTypeReference<>() {};
	private static final ParameterizedTypeReference<PayriffResponse<Object>> EMPTY_RESPONSE =
			new ParameterizedTypeReference<>() {};
	private static final ParameterizedTypeReference<PayriffResponse<PayriffOrderInfo>> ORDER_INFO_RESPONSE =
			new ParameterizedTypeReference<>() {};

	/** Payriff's idempotency header for creating an order; must be a UUID. */
	static final String REQUEST_RRN = "X-REQUEST-RRN";

	/** Reads Payriff's envelope from error bodies; Payriff adds fields (route, responseId) we do not map. */
	private static final ObjectMapper JSON = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private final RestClient restClient;

	public PayriffClient(RestClient.Builder builder, @Value("${masapay.payriff.base-url}") String baseUrl,
			@Value("${masapay.payriff.app-key}") String appKey) {
		this.restClient = builder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, appKey)
				.build();
	}

	/** Creates a Payriff order and returns where the customer pays it. */
	public PayriffOrder createOrder(PayriffCreateOrderRequest request) {
		return createOrder(request, null);
	}

	/**
	 * Same, with {@code requestRrn} (a UUID) as Payriff's idempotency key: asking again with the
	 * same RRN, e.g. after a timeout, returns the same order instead of opening a second one.
	 */
	public PayriffOrder createOrder(PayriffCreateOrderRequest request, String requestRrn) {
		PayriffResponse<PayriffOrder> response = call("createOrder", () -> restClient.post()
				.uri("/api/v3/orders")
				.headers(headers -> {
					if (requestRrn != null) {
						headers.set(REQUEST_RRN, requestRrn);
					}
				})
				.body(request)
				.retrieve()
				.body(ORDER_RESPONSE));
		if (response.payload() == null) {
			throw new PayriffException("createOrder", 200, response.code(), "no payload");
		}
		return response.payload();
	}

	/** The order as Payriff has it now, e.g. to learn whether it was paid. */
	public PayriffOrderInfo getOrder(String orderId) {
		PayriffResponse<PayriffOrderInfo> response = call("getOrder", () -> restClient.get()
				.uri("/api/v3/orders/{orderId}", orderId)
				.retrieve()
				.body(ORDER_INFO_RESPONSE));
		if (response.payload() == null) {
			throw new PayriffException("getOrder", 200, response.code(), "no payload");
		}
		return response.payload();
	}

	/** Refunds a paid order (fully, or partly when {@code amount} is less than what was paid). */
	public void refund(PayriffRefundRequest request) {
		call("refund", () -> restClient.post()
				.uri("/api/v3/refund")
				.body(request)
				.retrieve()
				.body(EMPTY_RESPONSE));
	}

	/** Runs one call and turns every kind of failure into a {@link PayriffException}. */
	private static <T> PayriffResponse<T> call(String operation, Supplier<PayriffResponse<T>> exchange) {
		PayriffResponse<T> response;
		try {
			response = exchange.get();
		}
		catch (RestClientResponseException e) {
			PayriffResponse<?> body = envelope(e.getResponseBodyAsString());
			throw new PayriffException(operation, e.getStatusCode().value(), body == null ? null : body.code(),
				body == null ? e.getStatusText() : body.message());
		}
		catch (RestClientException e) {
			// No answer (timeout, refused connection) or one that is not Payriff's JSON.
			throw new PayriffException(operation, 0, null, e.getMessage());
		}
		if (response == null) {
			throw new PayriffException(operation, 200, null, "empty response");
		}
		if (!response.succeeded()) {
			throw new PayriffException(operation, 200, response.code(), response.message());
		}
		return response;
	}

	/** Payriff's envelope from an error body, or null when the body is not one (e.g. a proxy page). */
	private static PayriffResponse<?> envelope(String body) {
		try {
			return JSON.readValue(body, PayriffResponse.class);
		}
		catch (Exception e) {
			return null;
		}
	}
}
