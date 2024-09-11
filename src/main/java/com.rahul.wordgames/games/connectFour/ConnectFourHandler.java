package com.rahul.wordgames.games.connectFour;

import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.games.HelperMethods;
import com.rahul.wordgames.repos.UserRepository;
import com.rahul.wordgames.games.GameHandler;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class ConnectFourHandler implements GameHandler {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HelperMethods helperMethods;

    private final ConcurrentHashMap<String, ConnectFour> sessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Player> matchMakingQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void handleTextMessage(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers){
        String command = payload.getString("command");
        
        switch (command){
            case "start":
                handleStart(session, payload, activeUsers);
                break;
            case "move":
                handleMove(session, payload, activeUsers);
                break;
            case "quit":
                handleQuit(session, payload, activeUsers);
                break;
            
        }
    }

    private void handleQuit(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        String gameId =payload.getString("gameId");
        ConnectFour game = sessions.get(gameId);
        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }

        Player player1 = game.getRedPlayer();
        Player player2 = game.getYellowPlayer();

        if(player1.getSocket().equals(session)){
            helperMethods.sendGameOver(player2.getSocket(), player1.getUsername() + " has quit");
            helperMethods.sendGameOver(player1.getSocket(), player1.getUsername() + " has quit");
        }
        else{
            helperMethods.sendGameOver(player1.getSocket(), player2.getUsername() + " has quit");
            helperMethods.sendGameOver(player2.getSocket(), player2.getUsername() + " has quit");
        }

        sessions.remove(gameId);
        activeUsers.remove(game.getRedPlayer().getUserId());
        activeUsers.remove(game.getYellowPlayer().getUserId());
    }

    private void handleMove(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        String gameId = payload.getString("gameId");
        ConnectFour game = sessions.get(gameId);

        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }
        Player player = game.getCurrentPlayer();
        if(game.move(payload.getInt("column"), payload.getString("userId"))) {
            sendMovedMessage(game);
            if(game.checkForWin(player.getColor())){
                helperMethods.sendGameOver(session, "You Win");
                helperMethods.sendGameOver(game.getCurrentPlayer().getSocket(), "You Lose");

                sessions.remove(gameId);
                activeUsers.remove(game.getRedPlayer().getUserId());
                activeUsers.remove(game.getYellowPlayer().getUserId());
                return;
            }
            else{
                if(game.isGameOver()){
                    helperMethods.sendGameOver(session, "Draw Game");
                    helperMethods.sendGameOver(game.getCurrentPlayer().getSocket(), "Draw Game");
                    sessions.remove(gameId);
                    activeUsers.remove(game.getRedPlayer().getUserId());
                    activeUsers.remove(game.getYellowPlayer().getUserId());
                    return;
                }
            }
        }
        else{
            helperMethods.sendInvalidMessage(session, "move is invalid");
        }
    }


    private void sendMovedMessage(ConnectFour game) {
        Player player1 = game.getRedPlayer();
        Player player2 = game.getYellowPlayer();
        Player current = game.getCurrentPlayer();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "moved");
        jsonObject.put("playerName", current.equals(player1) ? player2.getUsername() : player1.getUsername() );
        jsonObject.put("color", current.equals(player1) ? "yellow" : "red" );
        jsonObject.put("column", game.getRecentMove() );
        jsonObject.put("board", game.getBoard());

        try {
            player1.getSocket().sendMessage(new TextMessage(jsonObject.toString()));
            player2.getSocket().sendMessage(new TextMessage(jsonObject.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void handleStart(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        //check if user is already in session
        if(activeUsers.containsKey(payload.getString("userId"))){
            helperMethods.sendInvalidMessage(session, "cancel current session to join queue");
            return;
        }
        //create new Player from message
        Player player = new Player(payload.getString("userId"),
                userRepository.findById(new ObjectId(payload.getString("userId"))).orElseThrow().getUsername(),
                session
        );

        matchMakingQueue.add(player);
        activeUsers.put(payload.getString("userId"), "Connect Four");

        //check if game can be started
        if (!checkAndStartMatchIfPossible() )
            helperMethods.sendWaitMessage(session);
    }


    private boolean checkAndStartMatchIfPossible() {
        if(matchMakingQueue.size() < 2) return false;

        Player player1 = matchMakingQueue.poll();
        Player player2 = matchMakingQueue.poll();

        String gameId = UUID.randomUUID().toString();
        sessions.put(gameId, new ConnectFour(player1, player2));
        sendStartGameMessage(player1, player2, gameId);
        sendStartGameMessage(player2, player1, gameId);

        return true;
    }

    private void sendStartGameMessage(Player player, Player opponent, String gameId) {
        JSONObject json = new JSONObject();
        json.put("type", "started");
        json.put("game", "Connect Four");
        json.put("gameId", gameId);
        json.put("opponentName", opponent.getUsername());
        json.put("color", player.getColor()=='R' ? "red" : "yellow");
        try {
            player.getSocket().sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void clean(WebSocketSession session, User user, ConcurrentHashMap<String,String> activeUsers, ConcurrentHashMap<String, String> pendingInvites){
        String userId = user.getId();
        activeUsers.remove(userId);

        matchMakingQueue.removeIf(player -> player.getUserId().equals(userId));

        String gameIdToRemove = null;
        for (Map.Entry<String, ConnectFour> entry : sessions.entrySet()) {
            ConnectFour game = entry.getValue();
            if (game.getRedPlayer().getUserId().equals(userId) || game.getYellowPlayer().getUserId().equals(userId)) {
                if (game.getRedPlayer().getSocket().isOpen() && !game.getRedPlayer().getSocket().equals(session)) {
                    activeUsers.remove(game.getRedPlayer().getUserId());
                    helperMethods.sendGameOver(game.getRedPlayer().getSocket(), "Opponent has disconnected");
                }
                // Check if the yellow player's session is open and not the disconnecting session
                if (game.getYellowPlayer()!=null && game.getYellowPlayer().getSocket().isOpen() && !game.getYellowPlayer().getSocket().equals(session)) {
                    activeUsers.remove(game.getYellowPlayer().getUserId());
                    helperMethods.sendGameOver(game.getYellowPlayer().getSocket(), "Opponent has disconnected");
                }
                gameIdToRemove = entry.getKey();
                break; // Stop searching once the game is found
            }
        }

        // Remove the game from the sessions map if it was found
        if (gameIdToRemove != null) {
            sessions.remove(gameIdToRemove);
            pendingInvites.remove(gameIdToRemove);

        }
    }


    @Override
    public String handleInvite(WebSocketSession session, WebSocketSession recipSession, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        Player player1 = new Player(payload.getString("userId"),
                userRepository.findById(new ObjectId(payload.getString("userId"))).orElseThrow().getUsername(),
                session
        );

        ConnectFour connectFour = new ConnectFour(player1);

        String gameId = UUID.randomUUID().toString();
        sessions.put(gameId , connectFour);

        helperMethods.sendWaitMessage(session);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "invited");
        jsonObject.put("senderUsername", player1.getUsername());
        jsonObject.put("game", "Connect Four");
        jsonObject.put("gameId", gameId);

        try {
            recipSession.sendMessage(new TextMessage(jsonObject.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return gameId;
    }

    @Override
    public void handleAccept(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers){
        ConnectFour game = sessions.get(payload.getString("gameId"));

        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }
        Player player2 = new Player(payload.getString("userId"),
                userRepository.findById(new ObjectId(payload.getString("userId"))).orElseThrow().getUsername(),
                session
        );
        game.setPlayer2(player2);

        sendStartGameMessage(game.getRedPlayer(), game.getYellowPlayer(), payload.getString("gameId"));
        sendStartGameMessage(game.getYellowPlayer(), game.getRedPlayer(), payload.getString("gameId"));
    }

    @Override
    public void handleDecline(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String,String> activeUsers){
        ConnectFour game = sessions.get(payload.getString("gameId"));
        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }

        helperMethods.sendGameOver(game.getRedPlayer().getSocket(), "The invite was declined");

        sessions.remove(payload.getString("gameId"));
        activeUsers.remove(game.getRedPlayer().getUserId());
    }
}
