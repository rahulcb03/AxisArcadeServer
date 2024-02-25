package com.rahul.wordgames.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rahul.wordgames.entities.FriendRequest;
import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.repos.FriendRequestRepository;
import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository; 
    private final UserRepository userRepository; 

    public void sendFriendRequest(String username, String recipUsername) {

        User requester = userRepository.findUserByUsername(username).orElseThrow();
        User recipient = userRepository.findUserByUsername(recipUsername).orElseThrow();
        //Check if the user has already sent a friend request to the recip
        
        if(friendRequestRepository.findByRequesterIdAndRecipientId(requester.getId(), recipient.getId()).isPresent())
            return;

        
        FriendRequest friendRequest = new FriendRequest(requester.getId(), recipient.getId());

        friendRequestRepository.insert(friendRequest);

    }
    
}
