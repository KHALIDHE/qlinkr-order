package ma.qlinkr.live;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ma.qlinkr.auth.CurrentUser;
import ma.qlinkr.user.Role;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Checks STOMP frames, because the HTTP security filter only sees the
 * WebSocket handshake, not the subscriptions sent over the socket.
 *
 * CONNECT: an optional "Authorization: Bearer ..." header is verified and
 * becomes the session's user. SUBSCRIBE: a business topic needs a staff or
 * owner token of that business; an order topic needs no token, because its
 * 128-bit random name is the secret. Anything else is refused.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Pattern BUSINESS_TOPIC = Pattern.compile("^/topic/business\\.(\\d+)\\.orders$");
    private static final Pattern ORDER_TOPIC = Pattern.compile("^/topic/orders\\.[A-Za-z0-9_-]{16,32}$");

    private final JwtDecoder jwtDecoder;

    public StompAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stomp = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (stomp == null || stomp.getCommand() == null) {
            return message;
        }
        if (stomp.getCommand() == StompCommand.CONNECT) {
            String header = stomp.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    Jwt jwt = jwtDecoder.decode(header.substring(7));
                    stomp.setUser(new JwtAuthenticationToken(jwt));
                } catch (JwtException e) {
                    throw new MessageDeliveryException("Invalid token");
                }
            }
        } else if (stomp.getCommand() == StompCommand.SUBSCRIBE) {
            String destination = stomp.getDestination() == null ? "" : stomp.getDestination();
            if (ORDER_TOPIC.matcher(destination).matches()) {
                return message;
            }
            Matcher business = BUSINESS_TOPIC.matcher(destination);
            if (business.matches() && stomp.getUser() instanceof JwtAuthenticationToken token) {
                CurrentUser user = CurrentUser.from(token.getToken());
                boolean member = user.businessId() != null
                    && user.businessId() == Long.parseLong(business.group(1))
                    && (user.role() == Role.OWNER || user.role() == Role.STAFF);
                if (member) {
                    return message;
                }
            }
            throw new MessageDeliveryException("Subscription refused: " + destination);
        } else if (stomp.getCommand() == StompCommand.SEND) {
            throw new MessageDeliveryException("Clients do not send messages on this socket");
        }
        return message;
    }
}
