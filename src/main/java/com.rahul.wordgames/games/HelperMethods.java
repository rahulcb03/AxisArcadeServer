package com.rahul.wordgames.games;

import com.rahul.wordgames.games.connectFour.Player;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;

@Component
public class HelperMethods {

    public void sendInvalidMessage(WebSocketSession session, String message){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("type", "invalid");
        jsonObject.put("message", message);

        try {
            session.sendMessage(new TextMessage(jsonObject.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendWaitMessage(WebSocketSession session) {
        JSONObject json = new JSONObject();
        json.put("type", "wait");

        try {
            session.sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendGameOver(WebSocketSession session, String message){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("type", "game over");
        jsonObject.put("message", message);

        try {
            session.sendMessage(new TextMessage(jsonObject.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
