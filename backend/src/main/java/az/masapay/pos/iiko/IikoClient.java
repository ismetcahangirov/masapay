package az.masapay.pos.iiko;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Thin HTTP client for the iiko Cloud API (#40). Knows URLs and JSON shapes, no business rules. */
@Component
public class IikoClient {

	private final RestClient restClient;

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
}
