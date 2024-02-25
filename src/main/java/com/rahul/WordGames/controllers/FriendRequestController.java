package com.rahul.wordgames.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rahul.wordgames.services.FriendRequestService;
import com.rahul.wordgames.services.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/freind-request")
@RequiredArgsConstructor
@CrossOrigin
public class FriendRequestController {
    private final JwtService jwtService; 
    private final FriendRequestService friendRequestService; 

    @PostMapping("/{recipUsername}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String recipUsername, HttpServletRequest request){
        String jwt = jwtService.extractToken(request);
        String username = jwtService.extractUsername(jwt);

        friendRequestService.sendFriendRequest(username, recipUsername);

        return ResponseEntity.ok().build();



    }


}
