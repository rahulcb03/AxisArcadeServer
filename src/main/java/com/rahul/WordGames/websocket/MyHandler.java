package com.rahul.wordgames.websocket;


import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.rahul.wordgames.services.GameService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MyHandler extends TextWebSocketHandler {
    
    private final GameService gameService; 
    private final Map<String, WebSocketSession> sessions = new HashMap<>(); 

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
            
            case "invite":
                WebSocketSession recipSession = sessions.get(jsonMessage.getJSONObject("payload").getString("recipUsername"));
                
                gameService.handleInvite(session, recipSession, jsonMessage.getJSONObject("payload"));
                break; 
            
            case "accept":
                gameService.handleAccept(session, jsonMessage.getJSONObject("payload"));
                break;
            case "decline":
                gameService.handleDecline(session, jsonMessage.getJSONObject("payload"));
                break;
            case "cancel":
                gameService.handleCancel(session, jsonMessage.getJSONObject("payload").getString("userId"));
                break;
            
        }


    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = session.getPrincipal().getName();

        sessions.put(username, session);
        System.out.println("Connection established with: " + username);


    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus ) throws IOException{
        String username = session.getPrincipal().getName();
        gameService.handleCancelUsername(session, username);
        
        System.out.println("Connection closed with session: " + username);

    }
}
