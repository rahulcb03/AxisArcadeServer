package com.rahul.wordgames.repos;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.rahul.wordgames.entities.Friend;

@Repository
public interface FriendRepository extends MongoRepository<Friend, ObjectId>{

    List<Friend> findFriendByUserId1OrUserId2(String userId1, String userId2);
    
}
