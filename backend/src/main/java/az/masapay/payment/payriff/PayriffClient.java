package az.masapay.payment.payriff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Thin HTTP client for the Payriff API (#33). Knows URLs and JSON shapes, no business rules. */
@Component
public class PayriffClient {

	private static final ParameterizedTypeReference<PayriffResponse<PayriffOrder>> ORDER_RESPONSE =
			new ParameterizedTypeReference<>() {};

	private final RestClient restClient;

	public PayriffClient(RestClient.Builder builder, @Value("${masapay.payriff.base-url}") String baseUrl,
			@Value("${masapay.payriff.app-key}") String appKey) {
		this.restClient = builder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, appKey)
				.build();
	}

	/** Creates a Payriff order and returns where the customer pays it. */
	public PayriffOrder createOrder(PayriffCreateOrderRequest request) {
		PayriffResponse<PayriffOrder> response = restClient.post()
				.uri("/api/v3/orders")
				.body(request)
				.retrieve()
				.body(ORDER_RESPONSE);
		if (response == null || !response.succeeded() || response.payload() == null) {
			throw new IllegalStateException("Payriff createOrder failed: "
					+ (response == null ? "empty response" : response.code() + " " + response.message()));
		}
		return response.payload();
	}
}
