package com.rahul.wordgames.services;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.rahul.wordgames.games.connectFour.ConnectFour;
import com.rahul.wordgames.games.connectFour.Player;
import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class GameService {

    private final UserRepository userRepository;
    private final ConcurrentHashMap<String, ConnectFour> sessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Player> matchMakingQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, String> activeUsers = new ConcurrentHashMap<>(); 
    

    public void handleStart(WebSocketSession socket, JSONObject payload) throws IOException {

        if(activeUsers.containsKey(payload.getString("userId"))){
            sendInvalidMessage(socket, "cancel current session to join queue again");
            return;
        }
        Player player = createPlayerFromJson(payload, socket);
        matchMakingQueue.add(player);
        activeUsers.put(payload.getString("userId"), "active");
        if (!checkAndStartMatchIfPossible() ) 
            sendWaitMessage(player);
    }

    private Player createPlayerFromJson(JSONObject jsonObject, WebSocketSession socket) {
        Player player = new Player(jsonObject.getString("userId"), 
            userRepository.findById(new ObjectId(jsonObject.getString("userId"))).orElseThrow().getUsername(),
            socket
        );

        return player;
    }

    private synchronized boolean checkAndStartMatchIfPossible() throws IOException {
        if(matchMakingQueue.size() >= 2) {
            Player player1 = matchMakingQueue.poll();
            Player player2 = matchMakingQueue.poll();
            startMatch(player1, player2);
            return true;
        }
        return false; 
    }

    private void startMatch(Player player1, Player player2) throws IOException {
        String gameId = UUID.randomUUID().toString();
        sessions.put(gameId, new ConnectFour(player1, player2));
        sendStartGameMessage(player1, player2, gameId);
        sendStartGameMessage(player2, player1, gameId);
    }

    private void sendStartGameMessage(Player player, Player opponent, String gameId) throws IOException {
        JSONObject json = new JSONObject();
        json.put("type", "started");
        json.put("gameId", gameId);
        json.put("opponentName", opponent.getUsername());
        json.put("color", player.getColor()=='R' ? "red" : "yellow");
        player.getSocket().sendMessage(new TextMessage(json.toString()));
    }

    private void sendWaitMessage(Player player) throws IOException{
        JSONObject json = new JSONObject();
        json.put("type", "wait");
        player.getSocket().sendMessage(new TextMessage(json.toString()));
    }


    public void handleMove(WebSocketSession session, JSONObject jsonObject) throws IOException  {
        
        ConnectFour game = sessions.get(jsonObject.get("gameId"));
        
        if(game == null){
            sendGameOver(session, "Game was terminated");
            return;
        }
        Player player = game.getCurrentPlayer();
        if(game.move(jsonObject.getInt("column"), jsonObject.getString("userId"))) {
            sendMovedMessage(game);
            if(game.checkForWin(player.getColor())){
                sendGameOver(session, "You Win");
                sendGameOver(game.getCurrentPlayer().getSocket(), "You Lose");

                sessions.remove(jsonObject.get("gameId"));
                activeUsers.remove(game.getRedPlayer().getUserId());
                activeUsers.remove(game.getYellowPlayer().getUserId());
                return;
            }
            else{
                if(game.isGameOver()){
                    sendGameOver(session, "Draw Game");
                    sendGameOver(game.getCurrentPlayer().getSocket(), "Draw Game");
                    sessions.remove(jsonObject.get("gameId"));
                    activeUsers.remove(game.getRedPlayer().getUserId());
                    activeUsers.remove(game.getYellowPlayer().getUserId());
                    return;
                }
            }
        }
        else{
            sendInvalidMessage(session, "move is invalid");
        }
    }

    private void sendGameOver(WebSocketSession session, String string) throws IOException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("type", "game over");
        jsonObject.put("message", string);
        
        session.sendMessage(new TextMessage(jsonObject.toString()));
        
    }

    private void sendMovedMessage(ConnectFour game) throws IOException{
        Player player1 = game.getRedPlayer();
        Player player2 = game.getYellowPlayer();
        Player current = game.getCurrentPlayer();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "moved");
        jsonObject.put("playerName", current.equals(player1) ? player2.getUsername() : player1.getUsername() );
        jsonObject.put("color", current.equals(player1) ? "yellow" : "red" );
        jsonObject.put("column", game.getRecentMove() );
        jsonObject.put("board", game.getBoard());

        player1.getSocket().sendMessage(new TextMessage(jsonObject.toString()));
        player2.getSocket().sendMessage(new TextMessage(jsonObject.toString()));

    }

    private void sendInvalidMessage(WebSocketSession session, String msg) throws IOException{
        

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("type", "invalid");
        jsonObject.put("message", msg);

        session.sendMessage(new TextMessage(jsonObject.toString()));
    }

    public void handleQuit(WebSocketSession session, JSONObject jsonObject) throws IOException {

        ConnectFour game = sessions.get(jsonObject.getString("gameId"));
        if(game == null){
            sendGameOver(session, "Game was terminated");
            return;
        }


        Player player1 = game.getRedPlayer();
        Player player2 = game.getYellowPlayer();
       
        
        if(player1.getSocket().equals(session)){
            sendGameOver(player2.getSocket(), player1.getUsername() + " has quit");
            sendGameOver(player1.getSocket(), player1.getUsername() + " has quit");
        }
        else{
            sendGameOver(player1.getSocket(), player2.getUsername() + " has quit");
            sendGameOver(player2.getSocket(), player2.getUsername() + " has quit");
        }

        sessions.remove(jsonObject.getString("gameId"));
        activeUsers.remove(game.getRedPlayer().getUserId());
        activeUsers.remove(game.getYellowPlayer().getUserId());
    }

    public void handleInvite(WebSocketSession session, WebSocketSession recipSession, JSONObject payload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        if(activeUsers.containsKey(userRepository.findUserByUsername(payload.getString("recipUsername")).orElseThrow().getId())){
            sendInvalidMessage(session, payload.getString("recipUsername")+ " is in a game");
            
            return; 
        }

        if(activeUsers.containsKey(payload.getString("userId"))){
            sendInvalidMessage(session,"cancel current session to send an invite");
            return;
        }
        if(recipSession == null){
            sendInvalidMessage(session, payload.getString("recipUsername")+" is not active");
            
            return;
        }

        Player player1 = createPlayerFromJson(payload, session);

        ConnectFour connectFour = new ConnectFour(player1);

        String gameId = UUID.randomUUID().toString();
        sessions.put(gameId , connectFour);

        sendWaitMessage(player1);
        activeUsers.put(payload.getString("userId"), "active");

        jsonObject.put("type", "invited");
        jsonObject.put("senderUsername", player1.getUsername());
        jsonObject.put("gameId", gameId);

        recipSession.sendMessage(new TextMessage(jsonObject.toString()));
    }

    public void handleAccept(WebSocketSession session, JSONObject payload) throws IOException{
        ConnectFour game = sessions.get(payload.getString("gameId"));

        if(game == null){
            sendGameOver(session, "Game was terminated");
            return;
        }
        Player player2 = createPlayerFromJson(payload, session);
        game.setPlayer2(player2);

        sendStartGameMessage(game.getRedPlayer(), game.getYellowPlayer(), payload.getString("gameId"));
        sendStartGameMessage(game.getYellowPlayer(), game.getRedPlayer(), payload.getString("gameId"));

    }

    public void handleDecline(WebSocketSession session, JSONObject payload) throws IOException{
        ConnectFour game = sessions.get(payload.getString("gameId"));
        if(game == null){
            sendGameOver(session, "Game was terminated");
            return;
        }

        sendGameOver(game.getRedPlayer().getSocket(), "The invite was declined");
        
        sessions.remove(payload.getString("gameId"));
        activeUsers.remove(game.getRedPlayer().getUserId());
    }

    public void cleanSessions( WebSocketSession session, String userId) throws IOException{
        
        if(activeUsers.containsKey(userId))
            activeUsers.remove(userId);

        matchMakingQueue.removeIf(player -> player.getUserId().equals(userId));

        String gameIdToRemove = null;
        for (Map.Entry<String, ConnectFour> entry : sessions.entrySet()) {
            ConnectFour game = entry.getValue();
            if (game.getRedPlayer().getUserId().equals(userId) || game.getYellowPlayer().getUserId().equals(userId)) {
                if (game.getRedPlayer().getSocket().isOpen() && !game.getRedPlayer().getSocket().equals(session)) {
                    activeUsers.remove(game.getRedPlayer().getUserId());
                    sendGameOver(game.getRedPlayer().getSocket(), "Opponent has disconnected");
                }
                // Check if the yellow player's session is open and not the disconnecting session
                if (game.getYellowPlayer()!=null && game.getYellowPlayer().getSocket().isOpen() && !game.getYellowPlayer().getSocket().equals(session)) {
                    activeUsers.remove(game.getYellowPlayer().getUserId());
                    sendGameOver(game.getYellowPlayer().getSocket(), "Opponent has disconnected");
                }
                gameIdToRemove = entry.getKey();
                break; // Stop searching once the game is found
            }
        }
    
        // Remove the game from the sessions map if it was found
        if (gameIdToRemove != null) {
            sessions.remove(gameIdToRemove);
        
        }
    }

    
    public void cleanBeforeDC(WebSocketSession session, String username) throws IOException{
        cleanSessions(session, userRepository.findUserByUsername(username).orElseThrow().getId());
    }

    public void handleCancel(WebSocketSession session, String userId) throws IOException{
        cleanSessions(session, userId);
        JSONObject obj = new JSONObject();
        obj.put("type", "canceled");
        session.sendMessage(new TextMessage(obj.toString()));
    }

    

}
