package com.rahul.wordgames.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rahul.wordgames.dto.Details;
import com.rahul.wordgames.entities.Friend;
import com.rahul.wordgames.services.FriendService;
import com.rahul.wordgames.services.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("api/v1/friend")
public class FriendController {
    private final FriendService friendService; 
    private final JwtService jwtService;

    @GetMapping("")
    public ResponseEntity<List<Details>> friends(HttpServletRequest request){
        String jwt = jwtService.extractToken(request);
        String username = jwtService.extractUsername(jwt);

        return ResponseEntity.ok(friendService.listOfFriends(username));
    }




}
