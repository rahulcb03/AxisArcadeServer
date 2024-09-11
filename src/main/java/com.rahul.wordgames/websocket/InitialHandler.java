package com.rahul.wordgames.websocket;


import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.games.GameHandler;
import com.rahul.wordgames.games.HelperMethods;
import com.rahul.wordgames.games.battleship.BattleshipHandler;
import com.rahul.wordgames.games.connectFour.ConnectFourHandler;
import com.rahul.wordgames.repos.UserRepository;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InitialHandler extends TextWebSocketHandler {
    //{username: session}
    private final ConcurrentHashMap<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();
    //{userId: game}
    private final ConcurrentHashMap<String, String> activeUsers = new ConcurrentHashMap<>();
    //{gameId: game}
    private final ConcurrentHashMap<String, String> pendingInvites = new ConcurrentHashMap<>();
    //{game: gameHandler}
    private final HashMap<String, GameHandler> games = new HashMap<>();
    
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HelperMethods helperMethods;

    @Autowired
    public InitialHandler(ConnectFourHandler connectFourHandler, BattleshipHandler battleshipHandler){
        games.put("Connect Four", connectFourHandler);
        games.put("Battleship", battleshipHandler);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, JSONException {
        JSONObject jsonMessage = new JSONObject(message.getPayload());
        System.out.println(jsonMessage.toString());
        String command = jsonMessage.getString("type");

        switch (command){
            case "invite":
                WebSocketSession recipSession = webSocketSessions.get(jsonMessage.getJSONObject("payload").getString("recipUsername"));
                handleInvite(session, recipSession, jsonMessage.getJSONObject("payload"));
                break;

            case "accept":
                handleAccept(session, jsonMessage.getJSONObject("payload"));
                break;
            case "decline":
                handleDecline(session, jsonMessage.getJSONObject("payload"));
                break;
            case "cancel":
                handleCancel(session,jsonMessage.getJSONObject("payload") );
                break;
            case "game":
                hadnleGameMessage(session, jsonMessage.getJSONObject("payload"));
                break;
        }
    }

    private void handleCancel(WebSocketSession session, JSONObject payload) {
        String userId = payload.getString("userId");
        String game = activeUsers.get(userId);
        GameHandler gameHandler = games.get(game);
        User user = userRepository.findById(new ObjectId(userId)).orElseThrow();
        gameHandler.clean(session, user,activeUsers,pendingInvites);
        JSONObject obj = new JSONObject();
        obj.put("type", "canceled");
        try {
            session.sendMessage(new TextMessage(obj.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void handleDecline(WebSocketSession session, JSONObject payload) {
        String gameId = payload.getString("gameId");
        String game = pendingInvites.remove(gameId);
        GameHandler gameHandler = games.get(game);
        gameHandler.handleDecline(session,payload,activeUsers);

    }

    private void handleAccept(WebSocketSession session, JSONObject payload) {
        String gameId=payload.getString("gameId");
        if(!pendingInvites.containsKey(gameId)){return;}

        String game = pendingInvites.remove(gameId);
        GameHandler gameHandler = games.get(game);
        gameHandler.handleAccept(session,payload,activeUsers);
    }

    private void handleInvite(WebSocketSession session, WebSocketSession recipSession, JSONObject payload) {
        String userId = payload.getString("userId");
        if(recipSession == null) {
            helperMethods.sendInvalidMessage(session, payload.getString("recipUsername")+" is not active");;
            return;
        }
        if(activeUsers.containsKey(userRepository.findUserByUsername(payload.getString("recipUsername")).orElseThrow().getId())){
            helperMethods.sendInvalidMessage(session, payload.getString("recipUsername")+ " is in a game");
            return;
        }
        if(activeUsers.containsKey(userId)){
            helperMethods.sendInvalidMessage(session,"cancel current session to send an invite");
            return;
        }

        String game = payload.getString("game");
        GameHandler gameHandler = games.get(game);
        String gameId = gameHandler.handleInvite(session, recipSession, payload,activeUsers);

        pendingInvites.put(gameId, game);
        activeUsers.put(userId, "Connect Four");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = session.getPrincipal().getName();
        webSocketSessions.put(username, session);

        System.out.println(username + " has connected");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus ) throws IOException{
        String username = session.getPrincipal().getName();
        User user = userRepository.findUserByUsername(username).orElseThrow();

        if(activeUsers.containsKey(user.getId())){
            String game = activeUsers.get(user.getId());
            GameHandler gameHandler = games.get(game);
            gameHandler.clean(session, user, activeUsers,pendingInvites);
        }
        webSocketSessions.remove(username);
        System.out.println(username + " has disconnected");
    }

    public void hadnleGameMessage(WebSocketSession session, JSONObject payload){
        String game = payload.getString("game");

        GameHandler gameHandler = games.get(game);
        gameHandler.handleTextMessage(session, payload, activeUsers);
    }
}
