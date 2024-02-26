package com.rahul.wordgames.services;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.rahul.wordgames.dto.Details;
import com.rahul.wordgames.entities.Friend;
import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.repos.FriendRepository;
import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository; 

    public List<Details> listOfFriends(String username){
        User user = userRepository.findUserByUsername(username).orElseThrow();
        List<Details> details = new ArrayList<>();
        List<Friend> friends = friendRepository.findFriendByUserId1OrUserId2(user.getId(), user.getId());

        for(Friend fr: friends){

            String friendId = fr.getUserId1().equals(user.getId()) ? fr.getUserId2() : fr.getUserId1();

            details.add(new Details(
                fr.getId(),
                friendId, 
                userRepository.findById(new ObjectId(friendId)).orElseThrow().getUsername() 
                )
            );
        }

        return details;
    }
}
