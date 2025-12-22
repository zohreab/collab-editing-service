package com.collab.docservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-docs")
                // Use setAllowedOriginPatterns("*") to allow everything for testing
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Messages starting with /app are sent to our @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Messages starting with /topic are sent directly to the clients
        config.enableSimpleBroker("/topic");
    }
}