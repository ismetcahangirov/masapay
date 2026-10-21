package az.masapay.config;

import az.masapay.security.StompAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket. Customers connect to {@code /ws} and subscribe to their
 * table topic {@code /topic/tables/{qrToken}} to receive live order updates. An
 * in-memory simple broker fans messages out to the connected clients of this
 * instance; cross-instance delivery is handled by the Redis relay (#11, #21). Staff topics
 * ({@code /topic/restaurants/...}) require a signed-in subscriber.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthInterceptor stompAuthInterceptor;

	public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor) {
		this.stompAuthInterceptor = stompAuthInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Origins are unrestricted because the qrToken in the subscription path is the capability.
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
	}

	/** Staff-only topics need a signed-in subscriber (see {@link StompAuthInterceptor}). */
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthInterceptor);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}
}
