package com.rahul.wordgames.repos;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.rahul.wordgames.entities.FriendRequest;

@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, ObjectId> {
    
    Optional<FriendRequest> findByRequesterIdAndRecipientId(String requesterId, String recipientId);
    

}
