package com.rahul.wordgames.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.rahul.wordgames.websocket.AuthHandshakeInterceptor;
import com.rahul.wordgames.websocket.CustomHandshakeHandler;
import com.rahul.wordgames.websocket.MyHandler;

import lombok.RequiredArgsConstructor;


@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MyHandler myHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(myHandler, "/myHandler")
            .setHandshakeHandler(customHandshakeHandler)
            .addInterceptors(authHandshakeInterceptor)
            .setAllowedOrigins("*");
    }
}
