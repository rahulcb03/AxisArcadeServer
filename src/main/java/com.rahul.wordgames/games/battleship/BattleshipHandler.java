package com.rahul.wordgames.games.battleship;

import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.games.GameHandler;
import com.rahul.wordgames.games.HelperMethods;
import com.rahul.wordgames.games.connectFour.ConnectFour;
import com.rahul.wordgames.repos.UserRepository;
import org.bson.types.ObjectId;
import org.json.JSONArray;
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
public class BattleshipHandler implements GameHandler {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HelperMethods helperMethods;

    private final ConcurrentHashMap<String, Battleship> sessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Player> matchMakingQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void handleTextMessage(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {

        User user = userRepository.findUserByUsername(session.getPrincipal().getName()).orElseThrow();
        if(!user.getId().equals(payload.getString("userId"))){
            helperMethods.sendInvalidMessage(session, "Invalid user");
            return;
        }

        String command = payload.getString("command");

        switch (command){
            case "start":
                handleStart(session,payload,activeUsers);
                break;
            case "setup":
                handleSetup(session,payload);
                break;
            case "fire":
                handleFire(session,payload,activeUsers);
                break;
            case"quit":
                handleQuit(session,payload,activeUsers);
                break;
        }
    }

    private void handleSetup(WebSocketSession session, JSONObject payload) {
        String gameId = payload.getString("gameId");
        String userId = payload.getString("userId");
        JSONArray ships = payload.getJSONArray("ships");

        if(ships.length() != 5){
            helperMethods.sendInvalidMessage(session,"Incorrect number of ships for setup");
            return;
        }
        Battleship battleship = sessions.get(gameId);

        if(battleship==null){
            helperMethods.sendGameOver(session,"Game was terminated");
            return;
        }

        String[] cords = new String[5];
        String[] orientations = new String[5];
        String[] boats = new String[5];

        for(int i=0; i<ships.length(); i++){
            JSONObject ship = ships.getJSONObject(i);
            cords[i] = ship.getString("startPosition");
            orientations[i] = ship.getString("orientation");
            boats[i] = ship.getString("ship");
        }

        if(battleship.setup(userId,cords,orientations,boats)){
            String readyStatus="";

            JSONObject j = new JSONObject();
            j.put("type", "wait");
            String [][] ocean = battleship.getPlayer1().getUserId().equals(userId) ? battleship.getPlayer1OceanBoard() : battleship.getPlayer2OceanBoard();
            j.put("oceanBoard", ocean);
            try {
                session.sendMessage(new TextMessage(j.toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(battleship.getPlayer1Setup() && battleship.getPlayer2Setup()){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "readyToFire");
                jsonObject.put("status","fire");
                try {
                    battleship.getPlayer1().getSocket().sendMessage(new TextMessage(jsonObject.toString()));
                    jsonObject.put("status", "wait");
                    battleship.getPlayer2().getSocket().sendMessage(new TextMessage(jsonObject.toString()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }else{
            helperMethods.sendInvalidMessage(session, "Error during setup");
        }

    }

    private void handleQuit(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        String gameId =payload.getString("gameId");
        Battleship game = sessions.get(gameId);
        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }

        Player player1 = game.getPlayer1();
        Player player2 = game.getPlayer2();

        if(player1.getSocket().equals(session)){
            helperMethods.sendGameOver(player2.getSocket(), player1.getUsername() + " has quit");
            helperMethods.sendGameOver(player1.getSocket(), player1.getUsername() + " has quit");
        }
        else{
            helperMethods.sendGameOver(player1.getSocket(), player2.getUsername() + " has quit");
            helperMethods.sendGameOver(player2.getSocket(), player2.getUsername() + " has quit");
        }

        sessions.remove(gameId);
        activeUsers.remove(player1.getUserId());
        activeUsers.remove(player2.getUserId());
    }

    private void handleFire(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        String userId = payload.getString("userId");
        String gameId = payload.getString("gameId");
        String cord = payload.getString("cord");

        Battleship battleship = sessions.get(gameId);

        int res = battleship.fire(userId, cord);

        Player player = battleship.getCurrentPlayer() == battleship.getPlayer1() ?battleship.getPlayer2(): battleship.getPlayer1();
        Player opp = battleship.getCurrentPlayer();

        switch (res){
            case -1: //invalid
                helperMethods.sendInvalidMessage(session, "move is invalid");
                break;
            case 0: //miss
                sendFiredMessage(player, opp, "miss", cord, battleship);
                break;
            case 1: //hit
                sendFiredMessage(player, opp, "hit", cord, battleship);
                break;
            case 2://hit and sink
                sendFiredMessage(player, opp, "sink", cord, battleship);
                if(battleship.checkForWin(userId)){
                    helperMethods.sendGameOver(session, "You WIN");
                    helperMethods.sendGameOver(opp.getSocket(), "You LOSE");
                    sessions.remove(gameId);
                    activeUsers.remove(player.getUserId());
                    activeUsers.remove(opp.getUserId());
                }
                break;
        }

    }

    private void sendFiredMessage(Player player, Player opponent, String status, String cord, Battleship battleship){
        char[][] targetBoard = battleship.getCurrentPlayer().getPlayer() == 1 ? battleship.getPlayer2TargetBoard() : battleship.getPlayer1TargetBoard();
        String[][] oceanBoard = battleship.getCurrentPlayer().getPlayer() == 1 ? battleship.getPlayer1OceanBoard() : battleship.getPlayer2OceanBoard();

        JSONObject toPlayer = new JSONObject();
        toPlayer.put("type", status);
        toPlayer.put("cord", cord );
        toPlayer.put("targetBoard", targetBoard);

        JSONObject toOpponent = new JSONObject();
        toOpponent.put("type", "opponent "+status);
        toOpponent.put("cord", cord);
        toOpponent.put("oceanBoard", oceanBoard);

        try {
            player.getSocket().sendMessage(new TextMessage(toPlayer.toString()));
            opponent.getSocket().sendMessage(new TextMessage(toOpponent.toString()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleStart(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        //check if user is already in session
        String userId = payload.getString("userId");
        if(activeUsers.containsKey(userId)){
            helperMethods.sendInvalidMessage(session, "cancel current session to join queue");
            return;
        }
        //create new Player from message
        Player player = new Player(userId,
                userRepository.findById(new ObjectId(userId)).orElseThrow().getUsername(),
                session
        );

        matchMakingQueue.add(player);
        activeUsers.put(payload.getString("userId"), "Battleship");

        //check if game can be started
        if(matchMakingQueue.size()<2){
            helperMethods.sendWaitMessage(session);
        }else{
            Player player1 = matchMakingQueue.poll();
            Player player2 = matchMakingQueue.poll();

            String gameId = UUID.randomUUID().toString();
            sessions.put(gameId, new Battleship(player1, player2));
            sendStartGameMessage(player1, player2, gameId);
            sendStartGameMessage(player2, player1, gameId);
        }
    }


    @Override
    public String handleInvite(WebSocketSession session, WebSocketSession recipSession, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        Player player1 = new Player(payload.getString("userId"),
                userRepository.findById(new ObjectId(payload.getString("userId"))).orElseThrow().getUsername(),
                session
        );

        Battleship battleship = new Battleship(player1);

        String gameId = UUID.randomUUID().toString();
        sessions.put(gameId , battleship);

        helperMethods.sendWaitMessage(session);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "invited");
        jsonObject.put("senderUsername", player1.getUsername());
        jsonObject.put("game", "BattleShip");
        jsonObject.put("gameId", gameId);

        try {
            recipSession.sendMessage(new TextMessage(jsonObject.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return gameId;
    }

    @Override
    public void handleAccept(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        Battleship game = sessions.get(payload.getString("gameId"));

        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }
        Player player2 = new Player(payload.getString("userId"),
                userRepository.findById(new ObjectId(payload.getString("userId"))).orElseThrow().getUsername(),
                session
        );
        game.setPlayer2(player2);

        sendStartGameMessage(game.getPlayer1(), game.getPlayer2(), payload.getString("gameId"));
        sendStartGameMessage(game.getPlayer2(), game.getPlayer1(), payload.getString("gameId"));

    }

    private void sendStartGameMessage(Player player1, Player player2, String gameId) {
        JSONObject json = new JSONObject();
        json.put("type", "started");
        json.put("game", "Battleship");
        json.put("gameId", gameId);
        json.put("opponentName", player2.getUsername());
        json.put("player", player1.getPlayer()==1 ? "one" : "two");
        try {
            player1.getSocket().sendMessage(new TextMessage(json.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleDecline(WebSocketSession session, JSONObject payload, ConcurrentHashMap<String, String> activeUsers) {
        Battleship game = sessions.get(payload.getString("gameId"));
        if(game == null){
            helperMethods.sendGameOver(session, "Game was terminated");
            return;
        }

        helperMethods.sendGameOver(game.getPlayer1().getSocket(), "The invite was declined");

        sessions.remove(payload.getString("gameId"));
        activeUsers.remove(game.getPlayer1().getUserId());
    }


    @Override
    public void clean(WebSocketSession session, User user, ConcurrentHashMap<String, String> activeUsers, ConcurrentHashMap<String, String> pendingInvites) {
        String userId = user.getId();
        activeUsers.remove(userId);

        matchMakingQueue.removeIf(player -> player.getUserId().equals(userId));

        String gameIdToRemove = null;
        for (Map.Entry<String, Battleship> entry : sessions.entrySet()) {
            Battleship game = entry.getValue();
            if (game.getPlayer1().getUserId().equals(userId) || game.getPlayer2().getUserId().equals(userId)) {
                if (game.getPlayer1().getSocket().isOpen() && !game.getPlayer1().getSocket().equals(session)) {
                    activeUsers.remove(game.getPlayer1().getUserId());
                    helperMethods.sendGameOver(game.getPlayer1().getSocket(), "Opponent has disconnected");
                }
                // Check if the yellow player's session is open and not the disconnecting session
                if (game.getPlayer2()!=null && game.getPlayer2().getSocket().isOpen() && !game.getPlayer2().getSocket().equals(session)) {
                    activeUsers.remove(game.getPlayer2().getUserId());
                    helperMethods.sendGameOver(game.getPlayer2().getSocket(), "Opponent has disconnected");
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
}
