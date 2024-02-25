package com.rahul.wordgames.controllers;

import java.util.Optional;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rahul.wordgames.dto.JwtAuthResponse;
import com.rahul.wordgames.dto.RefreshTokenRequest;
import com.rahul.wordgames.dto.SignInRequest;
import com.rahul.wordgames.dto.SignUpRequest;

import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.services.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authenticationService; 

    @PostMapping("/signup")
    public ResponseEntity<User> signUp(@RequestBody SignUpRequest signUpRequest){
        Optional<User> user = authenticationService.signup(signUpRequest);

        if(user.isPresent()){
            return ResponseEntity.ok(user.get());
        } else{
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthResponse> signin(@RequestBody SignInRequest signInRequest){
        Optional<JwtAuthResponse> jwtAuthResponse = authenticationService.signin(signInRequest);
        return jwtAuthResponse.isPresent() ? ResponseEntity.ok(jwtAuthResponse.get()) : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest){
        
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }
    
}
