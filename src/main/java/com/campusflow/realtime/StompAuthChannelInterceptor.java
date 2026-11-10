package com.campusflow.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Security for STOMP frames (the HTTP handshake itself is public):
 * <ul>
 *   <li>CONNECT must carry a valid JWT; the session's user name becomes the user id, so
 *       {@code /user/queue/...} destinations resolve to that user only</li>
 *   <li>SUBSCRIBE is only allowed to the user's own queues</li>
 *   <li>SEND is refused: commands go through the REST API</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (accessor.getUser() == null || destination == null
                    || !destination.startsWith(WebSocketConfig.USER_QUEUE_PREFIX)) {
                throw new AccessDeniedException("You can only subscribe to your own queues");
            }
        } else if (command == StompCommand.SEND) {
            throw new AccessDeniedException("Sending over WebSocket is not supported; use the REST API");
        }
        return message;
    }

    private AbstractAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Missing bearer token in CONNECT frame");
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring(BEARER_PREFIX.length()));
            return jwtAuthenticationConverter.convert(jwt);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid or expired access token");
        }
    }
}
