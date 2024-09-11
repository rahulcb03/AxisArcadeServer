package com.rahul.wordgames.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.rahul.wordgames.services.JwtService;
import com.rahul.wordgames.services.UserService;
import com.mongodb.lang.NonNull;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request, 
        @NonNull HttpServletResponse response, 
        @NonNull FilterChain filterChain
    ) throws IOException, ExpiredJwtException, ServletException {

        try{
            final String authHeader = request.getHeader("Authorization");
            final String jwt; 
            final String username; 

            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                filterChain.doFilter(request, response);
                return; 
            }

            jwt = authHeader.substring(7);
            username = jwtService.extractUsername(jwt);
            
            if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);
                
                if(jwtService.isTokenValid(jwt, userDetails)){
                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    securityContext.setAuthentication(token);
                    SecurityContextHolder.setContext(securityContext);
                }
                
            }

        }
        catch(ExpiredJwtException error){
            
            response.setStatus(401);
            return; 
        }
        

        filterChain.doFilter(request, response);
        
        

    }

    
    

}