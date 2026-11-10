package com.campusflow.realtime;

import com.campusflow.security.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket at {@code /ws}, used only to push events to clients:
 * <ul>
 *   <li>{@code /user/queue/messages} - new chat messages</li>
 *   <li>{@code /user/queue/notifications} - new notifications</li>
 * </ul>
 * Clients send the JWT in the STOMP CONNECT frame ({@code Authorization: Bearer ...}); see
 * {@link StompAuthChannelInterceptor}. Commands (sending a message, ...) use the REST API.
 * <p>
 * The in-memory broker works for one application instance; several instances would need an
 * external broker (e.g. RabbitMQ) so a message reaches users connected to another instance.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws";
    public static final String USER_QUEUE_PREFIX = "/user/queue/";
    public static final String MESSAGES_QUEUE = "/queue/messages";
    public static final String NOTIFICATIONS_QUEUE = "/queue/notifications";

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Browsers send an Origin header; only our frontends may open a socket (mobile apps send none)
        registry.addEndpoint(ENDPOINT)
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
