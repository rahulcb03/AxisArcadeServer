package com.rahul.wordgames.services;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.rahul.wordgames.games.ConnectFour;
import com.rahul.wordgames.games.Player;
import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class GameService {

    private final UserRepository userRepository;
    private final ConcurrentHashMap<String, ConnectFour> sessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Player> matchMakingQueue = new ConcurrentLinkedQueue<>();
    

    public void handleStart(WebSocketSession socket, JSONObject jsonObject) throws IOException {
        Player player = createPlayerFromJson(jsonObject, socket);
        matchMakingQueue.add(player);
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
                return;
            }
            else{
                if(game.isGameOver()){
                    sendGameOver(session, "Draw Game");
                    sendGameOver(game.getCurrentPlayer().getSocket(), "Draw Game");
                    sessions.remove(jsonObject.get("gameId"));
                    return;
                }
            }
        }
        else{
            sendInvalidMessage(game);
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

        player1.getSocket().sendMessage(new TextMessage(jsonObject.toString()));
        player2.getSocket().sendMessage(new TextMessage(jsonObject.toString()));

    }

    private void sendInvalidMessage(ConnectFour game) throws IOException{
        Player player = game.getCurrentPlayer();

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("type", "invalid");
        jsonObject.put("message", "move is Invalid");

        player.getSocket().sendMessage(new TextMessage(jsonObject.toString()));
    }

    public void handleQuit(WebSocketSession session, JSONObject jsonObject) throws IOException {
        ConnectFour game = sessions.get(jsonObject.getString("gameId"));

        Player player1 = game.getRedPlayer();
        Player player2 = game.getYellowPlayer();
       
        
        if(player1.getSocket().equals(session)){
            sendGameOver(player2.getSocket(), player1.getUsername() + " has quit");
        }
        else{
            sendGameOver(player1.getSocket(), player2.getUsername() + " has quit");
        }

        sessions.remove(jsonObject.getString("gameId"));
    }

    public void handleInvite(WebSocketSession session, WebSocketSession recipSession, JSONObject payload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        if(recipSession == null){
            jsonObject.put("type", "invalid");
            jsonObject.put("message", "player is not active");
            session.sendMessage(new TextMessage(jsonObject.toString()));
            return;
        }

        Player player1 = createPlayerFromJson(payload, session);

        ConnectFour connectFour = new ConnectFour(player1);

        String gameId = UUID.randomUUID().toString();
        sessions.put(gameId , connectFour);

        sendWaitMessage(player1);

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
    }
    

}
