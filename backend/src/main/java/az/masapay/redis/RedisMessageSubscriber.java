package az.masapay.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Default subscriber on the masapay events channel. It logs every received
 * message, which makes the pub/sub wiring observable; the WebSocket/STOMP relay
 * that forwards these events to clients is added in issue #21.
 */
public class RedisMessageSubscriber implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel());
		String body = new String(message.getBody());
		log.info("Redis pub/sub message received on channel '{}': {}", channel, body);
	}
}
