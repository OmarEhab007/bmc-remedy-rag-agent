package com.bmc.rag.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebSocketConfig.
 */
@DisplayName("WebSocketConfig Tests")
class WebSocketConfigTest {

    @Test
    @DisplayName("configureMessageBroker enables simple broker with topic and queue")
    void configureMessageBroker_enablesSimpleBrokerWithTopicAndQueue() {
        WebSocketConfig config = new WebSocketConfig();
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class, RETURNS_DEEP_STUBS);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic", "/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }

    @Test
    @DisplayName("registerStompEndpoints registers ws-chat endpoint with SockJS")
    void registerStompEndpoints_registersWsChatEndpointWithSockJS() {
        WebSocketConfig config = new WebSocketConfig();
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);

        when(registry.addEndpoint("/ws-chat")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns(anyString(), anyString())).thenReturn(registration);

        config.registerStompEndpoints(registry);

        // Two endpoints registered: one with SockJS, one raw
        verify(registry, times(2)).addEndpoint("/ws-chat");
        verify(registration, times(2)).setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
        verify(registration, times(1)).withSockJS();
    }

    @Test
    @DisplayName("WebSocketConfig implements WebSocketMessageBrokerConfigurer")
    void webSocketConfig_implementsConfigurer() {
        WebSocketConfig config = new WebSocketConfig();
        assertThat(config).isInstanceOf(org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer.class);
    }
}
