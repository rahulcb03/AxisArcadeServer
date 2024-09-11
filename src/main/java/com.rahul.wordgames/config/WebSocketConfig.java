package com.rahul.wordgames.config;

import com.rahul.wordgames.websocket.InitialHandler;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
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
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private InitialHandler initialHandler;
    @Autowired
    private AuthHandshakeInterceptor authHandshakeInterceptor;
    @Autowired
    private CustomHandshakeHandler customHandshakeHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(initialHandler, "/myHandler")
            .setHandshakeHandler(customHandshakeHandler)
            .addInterceptors(authHandshakeInterceptor)
            .setAllowedOrigins("*");
    }
}
