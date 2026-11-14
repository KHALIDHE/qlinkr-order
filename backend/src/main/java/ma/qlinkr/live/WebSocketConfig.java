package ma.qlinkr.live;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket on /ws.
 *
 *   /topic/business.{id}.orders   staff screen of one business (needs a staff token)
 *   /topic/orders.{publicToken}   one customer's order (the unguessable token is the key)
 *
 * The in-memory simple broker is enough for one instance. With several
 * instances, a relay to RabbitMQ or Redis pub/sub would fan messages out.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor auth;
    private final String[] allowedOrigins;

    public WebSocketConfig(StompAuthInterceptor auth,
                           @Value("${qlinkr.websocket.allowed-origins:http://localhost:*}") String[] allowedOrigins) {
        this.auth = auth;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(auth);
    }
}
