package com.rahul.wordgames.websocket;


import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.rahul.wordgames.services.GameService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MyHandler extends TextWebSocketHandler {
    
    private final GameService gameService; 

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, JSONException {
        
        JSONObject jsonMessage = new JSONObject(message.getPayload());
        String command = jsonMessage.getString("type");

        switch (command){
            case "start":
                gameService.handleStart(session, jsonMessage.getJSONObject("payload"));
                break;

            case "move":
                gameService.handleMove(session, jsonMessage.getJSONObject("payload"));
                break;

            case "quit":
                gameService.handleQuit(session, jsonMessage.getJSONObject("payload"));
                break;
                            
            
        }


    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Logic to execute after connection is established
        System.out.println("Connection established with session: " + session.getId());
    }
}
