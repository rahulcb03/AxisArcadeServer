package com.rahul.wordgames.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rahul.wordgames.dto.UserProfile;
import com.rahul.wordgames.services.JwtService;
import com.rahul.wordgames.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/v1/user")
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class UserController {
    
    private final JwtService jwtService; 
    private final UserService userService;

    @GetMapping("")
    public ResponseEntity<UserProfile> getUserProfile(HttpServletRequest request){
        String jwt = jwtService.extractToken(request);
        String username = jwtService.extractUsername(jwt);

        UserProfile userProfile = userService.createProfile(username);

        return ResponseEntity.ok(userProfile);


    }
}
