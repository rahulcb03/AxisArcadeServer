package com.rahul.wordgames.games;

import com.rahul.wordgames.entities.User;
import org.json.JSONObject;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

public interface GameHandler {

    public void handleTextMessage(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers);
    public String handleInvite(WebSocketSession session, WebSocketSession recipSession, JSONObject payload, ConcurrentHashMap<String, String> activeUsers);
    public void handleAccept(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers);
    public void handleDecline(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String,String> activeUsers);
    public void clean(WebSocketSession session, User user, ConcurrentHashMap<String,String> activeUsers, ConcurrentHashMap<String, String> pendingInvites);

}
