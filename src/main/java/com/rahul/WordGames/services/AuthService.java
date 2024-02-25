package com.rahul.wordgames.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rahul.wordgames.dto.JwtAuthResponse;
import com.rahul.wordgames.dto.RefreshTokenRequest;
import com.rahul.wordgames.dto.SignInRequest;
import com.rahul.wordgames.dto.SignUpRequest;

import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.repos.UserRepository;



import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService{

    private final UserRepository userRepository; 
    private final PasswordEncoder passwordEncoder; 
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public Optional<User> signup(SignUpRequest signUpRequest){

        //Check if the email is already in use or there is already a user assocaited with that student
        if(userRepository.findUserByUsername(signUpRequest.getUsername()).isPresent() || userRepository.findUserByEmail(signUpRequest.getEmail()).isPresent()){
            return Optional.empty();
        }

        User user = new User(
            signUpRequest.getUsername(),
            passwordEncoder.encode(signUpRequest.getPassword()), 
            signUpRequest.getEmail(), 
            new ArrayList<String>()
        );

        userRepository.insert(user);

        return Optional.of(user); 
    }

    public Optional<JwtAuthResponse> signin( SignInRequest signInRequest){


        Optional<User> user = userRepository.findUserByUsername(signInRequest.getUsername());
       
        if(!user.isPresent())
            return Optional.empty();

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getUsername(), signInRequest.getPassword()));


        var jwt = jwtService.generateToken(user.get());
        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user.get());

        JwtAuthResponse jwtAuthResponse = new JwtAuthResponse();

        jwtAuthResponse.setToken(jwt);
        jwtAuthResponse.setRefreshToken(refreshToken);

        return Optional.of(jwtAuthResponse);
  
    }

    public JwtAuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest){
        String username = jwtService.extractUsername(refreshTokenRequest.getToken());
        User user = userRepository.findUserByUsername(username).orElseThrow(); 

        if(jwtService.isTokenValid(refreshTokenRequest.getToken(), user)){
            var jwt = jwtService.generateToken(user);

            JwtAuthResponse jwtAuthResponse = new JwtAuthResponse();

            jwtAuthResponse.setToken(jwt);
            jwtAuthResponse.setRefreshToken(refreshTokenRequest.getToken());

            return jwtAuthResponse;
        }

        return null; 
    }

    
  

}
