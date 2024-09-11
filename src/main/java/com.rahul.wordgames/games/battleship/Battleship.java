package com.rahul.wordgames.games.battleship;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

import java.util.HashMap;

public class Battleship {

    //oceanBoard = {"2-M","2-M",null,null,null,"3-H","3-H","3-H"}
    @Getter
    private String[][] player1OceanBoard = new String[10][10];
    @Getter
    private String[][] player2OceanBoard = new String[10][10];

    //TargetBoard = {'M','M','\u0000','\u0000','\u0000','H','H','H'}
    @Getter
    private char[][] player1TargetBoard = new char[10][10];
    @Getter
    private char[][] player2TargetBoard = new char[10][10];

    @Getter
    private Boolean player1Setup;
    @Getter
    private Boolean player2Setup;

    //BoatTracker = {"2B":1, "3S" :2, ...}
    private final HashMap<String,Integer> player1BoatTracker;
    private final HashMap<String,Integer> player2BoatTracker;

    @Getter
    private Player currentPlayer;

    @Getter
    private Player player1;
    @Getter
    private Player player2;

    public Battleship(Player player1, Player player2){
        this.player1 =player1;
        this.player2 = player2;
        currentPlayer = player1;
        player1BoatTracker = new HashMap<>();
        player2BoatTracker = new HashMap<>();
        initMap(player1BoatTracker);
        initMap(player2BoatTracker);
        player1.setPlayer(1);
        player2.setPlayer(2);
        player1Setup = false;
        player2Setup = false;

    }
    public Battleship(Player player1){
        currentPlayer = player1;
        this.player1 =player1;
        player1BoatTracker = new HashMap<>();
        player2BoatTracker = new HashMap<>();
        initMap(player1BoatTracker);
        initMap(player2BoatTracker);
        player1.setPlayer(1);
        player1Setup = false;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
        player2.setPlayer(2);
        player2Setup = false;
    }

    private void initMap(HashMap<String, Integer> map){
        map.put("2D",0);
        map.put("3C",0);
        map.put("3S",0);
        map.put("4B",0);
        map.put("5C",0);
    }

    public boolean setup(String userId, String[] cords, String[] orientations, String[] boats) {
        if (cords == null || orientations == null || boats == null || cords.length != orientations.length || cords.length != boats.length) {
            return false;  // Invalid input
        }
        // Determine which player's board to use
        String[][] oceanBoard = userId.equals(player1.getUserId()) ? player1OceanBoard : player2OceanBoard;

        boolean isSetup = userId.equals(player1.getUserId()) ? player1Setup : player2Setup;
        if(isSetup){
            return false;
        }

        for (int i = 0; i < cords.length; i++) {
            int[] c = calcCord(cords[i]);
            int row = c[0];
            int col = c[1];

            int length = Integer.parseInt(boats[i].substring(0,1));

            // Check orientation and place the ship accordingly
            if (orientations[i].equals("vertical")) {
                if (row + length > oceanBoard.length) {
                    return false; // Ship doesn't fit vertically
                }
                for (int x = 0; x < length; x++) {
                    if(oceanBoard[row+x][col]!=null)return false;
                    oceanBoard[row + x][col] = boats[i] + "-" + "M";
                }
            } else if (orientations[i].equals("horizontal")) {
                if (col + length > oceanBoard[0].length) {
                    return false; // Ship doesn't fit horizontally
                }
                for (int x = 0; x < length; x++) {
                    if(oceanBoard[row][col+x]!=null)return false;
                    oceanBoard[row][col + x] = boats[i] + "-" + "M";
                }
            } else {
                return false; // Invalid orientation
            }
        }

        if(player1.getUserId().equals(userId)){
            player1Setup =true;
        }else{
            player2Setup = true;
        }
        return true;
    }

    private int[] calcCord(String cord){
        int[] ans = new int[2];
        String[] s= cord.split(",");

        ans[0] = Integer.parseInt(s[0]);
        ans[1] = Integer.parseInt(s[1]);

        return ans;
    }
    // -1 = invalid, 0=miss, 1=hit, 2=sink
    public int fire(String userId, String cord){
        int ret=0;

        if(!currentPlayer.getUserId().equals(userId)) return -1;

        String[][] opponentOcean = currentPlayer.getPlayer()==1 ? player2OceanBoard : player1OceanBoard;
        char[][] playerTarget = currentPlayer.getPlayer()==1 ? player1TargetBoard : player2TargetBoard;

        HashMap<String,Integer> map = currentPlayer.getPlayer()==1?player1BoatTracker:player2BoatTracker;

        int[] c = calcCord(cord);

        int row = c[0];
        int col = c[1];


        if(playerTarget[row][col] != '\u0000') {
            System.out.println("hello :"+ playerTarget[row][col]);
            return -1;
        }

        if(opponentOcean[row][col] != null){
            playerTarget[row][col] = 'H';
            opponentOcean[row][col] = opponentOcean[row][col].replace('M','H');
            String boat = opponentOcean[row][col].split("-")[0];
            map.put(boat,map.get(boat)+1);
            if(map.get(boat) == Integer.parseInt(boat.substring(0,1))){
                map.remove(boat);
                ret= 2;
            }
            else{
                ret= 1;
            }

        }else{
            playerTarget[row][col] = 'M';
        }

        currentPlayer = currentPlayer == player1 ? player2 :player1 ;
        return ret;
    }

    public boolean checkForWin(String userId){
        HashMap<String,Integer> map = player1.getUserId().equals(userId) ? player1BoatTracker : player2BoatTracker;
        return map.isEmpty();
    }
}
