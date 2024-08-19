package com.rahul.wordgames.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.rahul.wordgames.dto.Details;
import com.rahul.wordgames.entities.Friend;
import com.rahul.wordgames.entities.FriendRequest;
import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.repos.FriendRepository;
import com.rahul.wordgames.repos.FriendRequestRepository;
import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository; 
    private final UserRepository userRepository; 
    private final FriendRepository friendRepository;

    public String sendFriendRequest(String username, String recipUsername) {

        User requester = userRepository.findUserByUsername(username).orElseThrow();
        Optional<User> recip = userRepository.findUserByUsername(recipUsername);
        User recipient;
        if(!recip.isPresent()){
            return ("User not found");
        }
        recipient = recip.get();
        //Check if the user has already sent a friend request to the recip
        
        if(friendRequestRepository.findByRequesterIdAndRecipientId( recipient.getId(), requester.getId()).isPresent()){
            acceptFriendRequest(username, friendRequestRepository.findByRequesterIdAndRecipientId( recipient.getId(), requester.getId()).orElseThrow().getId());
            return("You are now friends with "+recipUsername);
        }
            

        if(friendRequestRepository.findByRequesterIdAndRecipientId(requester.getId(), recipient.getId()).isPresent())
            return("Friend request has already been sent to " + recipUsername);

        if(friendRepository.findFriendByUserId1AndUserId2(requester.getId(), recipient.getId()).isPresent() || 
        friendRepository.findFriendByUserId1AndUserId2(recipient.getId(), requester.getId()).isPresent()){
            return("You are already friends with "+ recipUsername); 
        }

        
        FriendRequest friendRequest = new FriendRequest(requester.getId(), recipient.getId());

        friendRequestRepository.insert(friendRequest);
        return("" );

    }

    public Optional<Friend> acceptFriendRequest(String username, String requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(new ObjectId(requestId)).orElseThrow();

        User user = userRepository.findUserByUsername(username).orElseThrow();

        if(!friendRequest.getRecipientId().equals(user.getId())){
            return Optional.empty();
        }

        Friend friend = new Friend(friendRequest.getRecipientId(), friendRequest.getRequesterId());

        friendRepository.insert(friend);

        friendRequestRepository.delete(friendRequest);

        return Optional.of(friend);
    }

    public List<Details> incomingFriendRequests(String username) {
        User user = userRepository.findUserByUsername(username).orElseThrow();

        List<FriendRequest> friendRequests = friendRequestRepository.findFriendRequestByRecipientId(user.getId());
        List<Details> details = new ArrayList<>();

        for(FriendRequest fr : friendRequests){
            details.add(new Details(
                fr.getId(), 
                fr.getRequesterId(), 
                userRepository.findById(new ObjectId(fr.getRequesterId())).orElseThrow().getUsername()  
                )
            );
        }

        return details;
    }


    
}
