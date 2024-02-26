package com.rahul.wordgames.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "Friend")
public class Friend {
    @Id
    private String Id; 

    private String userId1;
    private String userId2; 

    public Friend(String userId1, String userId2){
        this.userId1 = userId1;
        this.userId2 = userId2; 
    }
}
