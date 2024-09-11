package com.rahul.wordgames.games.battleship;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
@AllArgsConstructor
public class Player {
    private String userId;
    private String username;
    private WebSocketSession socket;
    private int player; //1 or 2

    public Player(String userId, String username, WebSocketSession socket){
        this.userId = userId;
        this.username = username;
        this.socket = socket;
    }
}
