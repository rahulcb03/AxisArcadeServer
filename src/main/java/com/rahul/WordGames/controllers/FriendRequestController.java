package com.rahul.wordgames.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rahul.wordgames.dto.Details;
import com.rahul.wordgames.entities.Friend;
import com.rahul.wordgames.entities.FriendRequest;
import com.rahul.wordgames.services.FriendRequestService;
import com.rahul.wordgames.services.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/friend-request")
@RequiredArgsConstructor
@CrossOrigin
public class FriendRequestController {

    private final JwtService jwtService; 
    private final FriendRequestService friendRequestService; 

    @GetMapping("/")
    public ResponseEntity<List<Details>> incomingFriendRequests(HttpServletRequest request){
        String jwt = jwtService.extractToken(request);
        String username = jwtService.extractUsername(jwt);

        List<Details> friendRequests = friendRequestService.incomingFriendRequests(username);

        return ResponseEntity.ok(friendRequests); 
    }

    @PostMapping("/{recipUsername}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String recipUsername, HttpServletRequest request){
        String jwt = jwtService.extractToken(request);
        String username = jwtService.extractUsername(jwt);

        friendRequestService.sendFriendRequest(username, recipUsername);

        return ResponseEntity.ok().build();

    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable String requestId, HttpServletRequest request){
        String jwt = jwtService.extractToken(request);
        String username = jwtService.extractUsername(jwt);

        Optional<Friend> friend = friendRequestService.acceptFriendRequest(username, requestId);
        
        return friend.isPresent() ? ResponseEntity.ok(friend.get()): 
            ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    


}
