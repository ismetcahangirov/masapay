package az.masapay.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Verifies the STOMP delivery path without external infrastructure: a client
 * connects, subscribes to a table topic, and receives a message pushed to that
 * destination. The Redis relay (which calls the same template) is unit-tested
 * separately; together they cover the full broadcast chain.
 */
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = {
		"spring.autoconfigure.exclude="
			+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
		"masapay.redis.enabled=false",
		"masapay.auth.enabled=false",
		"masapay.orders.enabled=false"
	})
class WebSocketStompIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Test
	void subscribedClientReceivesPushedMessage() throws Exception {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new MappingJackson2MessageConverter());

		StompSession session = client
			.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
			.get(5, TimeUnit.SECONDS);

		String qrToken = UUID.randomUUID().toString();
		BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
		session.subscribe("/topic/tables/" + qrToken, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void handleFrame(StompHeaders headers, Object payload) {
				received.add((Map<String, Object>) payload);
			}
		});
		// Give the broker a moment to register the subscription before publishing.
		Thread.sleep(300);

		messagingTemplate.convertAndSend("/topic/tables/" + qrToken, Map.of("status", "OPEN"));

		Map<String, Object> message = received.poll(5, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(message.get("status")).isEqualTo("OPEN");

		session.disconnect();
	}
}
