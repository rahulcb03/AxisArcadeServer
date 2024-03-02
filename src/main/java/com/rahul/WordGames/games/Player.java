package com.rahul.wordgames.games;

import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Player {
    private String userId;
    private String username;
    private WebSocketSession socket; 
}
