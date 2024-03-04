package com.rahul.wordgames.websocket;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.rahul.wordgames.services.JwtService;
import com.rahul.wordgames.services.UserService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Extract token from the query parameter
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        
        if (token != null && validateToken(token)) {
            // Token is valid, proceed with the handshake
            return true;
        } else {
            // Token is invalid, respond with unauthorized
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // Post-handshake logic (if necessary)
    }

    private boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            
            
            if (username != null && !username.isEmpty()) {
                UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);
                return jwtService.isTokenValid(token, userDetails);
            }
            return false;
        } catch (Exception e) {
            // Log the exception or handle it as needed
            return false;
        }
    }
}
