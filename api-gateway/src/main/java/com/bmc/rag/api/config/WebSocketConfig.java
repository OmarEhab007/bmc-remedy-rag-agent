package com.bmc.rag.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time chat streaming.
 * Uses STOMP protocol over WebSocket with SockJS fallback.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // /topic - for broadcast messages
        // /queue - for user-specific messages
        config.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws-chat")
            .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
            .withSockJS();

        // Also register a raw WebSocket endpoint (without SockJS) for clients that support it
        registry.addEndpoint("/ws-chat")
            .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
