package com.rahul.wordgames.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "FriendRequest")
@Data
public class FriendRequest {
    
    @Id
    private String id; 

    private String requesterId;
    private String recipientId;

   

    public FriendRequest(String requesterId, String recipientId){
        this.requesterId = requesterId;
        this.recipientId = recipientId;
    
    }
}
