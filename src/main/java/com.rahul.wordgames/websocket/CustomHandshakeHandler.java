package com.rahul.wordgames.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.rahul.wordgames.services.JwtService;
import com.rahul.wordgames.services.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtService jwtService;
    private final UserService userService; 

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");

        if (token != null) {
            try {
                String username = jwtService.extractUsername(token);

                if (username != null && !username.isEmpty()) {
                    UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);
                    return new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
                }
                return null;

            } catch (Exception e) {
            
                return null;
            }
        } 
            
        return null;
        
        
    }
}
