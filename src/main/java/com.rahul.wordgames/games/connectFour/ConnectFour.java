package com.rahul.wordgames.games.connectFour;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ConnectFour {


    @Getter
    private char[][] board= new char[6][7];
    private char currentPlayer; 
    private Map<Character, Player> players;
    @Getter
    private int recentMove;
     
    public ConnectFour(Player player){
        currentPlayer = 'R';
        players = new HashMap<>();
        players.put('R', player);
        player.setColor('R');

        for(int x=0; x<board.length; x++)
            Arrays.fill(board[x], '.');
    }
    public ConnectFour(Player player1, Player player2){
        currentPlayer = 'R';
        players = new HashMap<>();
        players.put('R', player1);
        players.put('Y', player2);
        player1.setColor('R');
        player2.setColor('Y');
        

        for(int x=0; x<board.length; x++)
            Arrays.fill(board[x], '.');
    }

    public void setPlayer2(Player player){
        if (players.size()==2)
            return;
        players.put('Y', player);
        player.setColor('Y');
    }

    public boolean move(int column, String userId){
        if(players.size()!= 2)
            return false; 
        //gaurd to make sure colum is in bounds
        if(column >= board[0].length || column<0){
            return false; 
        }

        if(!players.get(currentPlayer).getUserId().equals(userId)){
            return false; 
        }

        //traversefind the first open index
        for(int x=board.length-1; x>=0; x--){

            if(board[x][column] == '.'){
                board[x][column] = currentPlayer;
                currentPlayer = currentPlayer == 'R' ? 'Y' : 'R';
                recentMove = column;
                return true; 
            }
        }

        return false; 
    }

    public boolean checkForWin(char token){

        // Check all rows for a horizontal win
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length - 3; col++) {
                if (board[row][col] == token
                        && board[row][col + 1] == token
                        && board[row][col + 2] == token
                        && board[row][col + 3] == token) {
                    return true;
                }
            }
        }

        // Check all columns for a vertical win
        for (int col = 0; col < board[0].length; col++) {
            for (int row = 0; row < board.length - 3; row++) {
                if (board[row][col] == token
                        && board[row + 1][col] == token
                        && board[row + 2][col] == token
                        && board[row + 3][col] == token) {
                    return true;
                }
            }
        }

        // Check for a diagonal win (bottom left to top right)
        for (int row = 3; row < board.length; row++) {
            for (int col = 0; col < board[0].length - 3; col++) {
                if (board[row][col] == token
                        && board[row - 1][col + 1] == token
                        && board[row - 2][col + 2] == token
                        && board[row - 3][col + 3] == token) {
                    return true;
                }
            }
        }

        // Check for a diagonal win (top left to bottom right)
        for (int row = 0; row < board.length - 3; row++) {
            for (int col = 0; col < board[0].length - 3; col++) {
                if (board[row][col] == token
                        && board[row + 1][col + 1] == token
                        && board[row + 2][col + 2] == token
                        && board[row + 3][col + 3] == token) {
                    return true;
                }
            }
        }

        return false; // No win found

    }

    public boolean isGameOver(){
        for(char c: board[0]){
            if(c == '.')
                return false; 
        }
        return true; 
    }

    public Player getCurrentPlayer(){
        return players.get(currentPlayer);
    }

    public Player getRedPlayer() {
        return players.get('R');
    }

    public Player getYellowPlayer() {
        return players.get('Y');
    }
    
}
