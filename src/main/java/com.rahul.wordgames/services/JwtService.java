package com.rahul.wordgames.services;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;


@Service
public class JwtService{

    @Value("${jwt.secret.key}")
    private String sKey ;

    public JwtService(@Value("${JWT_SECRET_KEY}") String sKey ){
        this.sKey= sKey; 
    }

    public String generateToken(UserDetails userdetails){
        return Jwts.builder().setSubject(userdetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 1000*24*3600))
            .signWith(getSignInKey())
            .compact();

    }

    public String generateRefreshToken(HashMap<String, Object> claims, UserDetails userdetails){
        return Jwts.builder().setClaims(claims).setSubject(userdetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 1000*24*3600*7))
            .signWith(getSignInKey())
            .compact();

    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }
    

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token); 
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
            .parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSignInKey() {
        // System.out.println(sKey);
        byte[] secretKey = Decoders.BASE64.decode(sKey);
        return Keys.hmacShaKeyFor(secretKey);
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token );
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public String extractToken(HttpServletRequest request){
        final String authHeader = request.getHeader("Authorization");
        
        return authHeader.substring(7);
    }

   

}
