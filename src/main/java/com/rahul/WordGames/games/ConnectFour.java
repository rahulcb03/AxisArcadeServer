package com.rahul.wordgames.games;

import java.util.Arrays;
import java.util.Map;


public class ConnectFour {


    private char[][] board= new char[6][7];
    private char currentPlayer; 
    private Map<Character, Player> players; 
    private int recentMove;
     

    public ConnectFour(Player player1, Player player2){
        currentPlayer = 'R';
        
        players.put('R', player1);
        players.put('Y', player2);
        

        for(int x=0; x<board.length; x++)
            Arrays.fill(board[x], '.');
    }

    public int getRecentMove(){
        return recentMove;
    }

    public char[][] getBoard(){
        return board; 
    }

    public boolean move(int column, String userId){
        
        //gaurd to make sure colum is in bounds
        if(column >= board[0].length){
            return false; 
        }

        if(!players.get(currentPlayer).getUserId().equals(userId)){
            return false; 
        }

        //traverse board from bottom up, find the first open index
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

    public boolean checkForWin(){

        for(int x=0; x<board.length; x++){
            for(int y=0; y<board[0].length-3; y++){
                for(int i=0; i<4; i++){
                    if(board[x][y+i]!=currentPlayer)
                        break; 
                    if(i==3)
                        return true; 
                }
            }
        }

        for(int x=0; x<board[0].length; x++){
            for(int y=0; y<board.length-3; y++){
                for(int i=0; i<4; i++){
                    if(board[y+i][x]!=currentPlayer)
                        break; 
                    if(i==3)
                        return true; 
                }
            }
        }

        for(int x=0; x<board[0].length-3; x++){
            for(int y=0; y<board.length-3 && y+x<board[0].length-3; y++ ){
                for(int i=0; i<4; i++){
                    if(board[y+i][y+x+i]!=currentPlayer)
                        break; 
                    if(i==3)
                        return true; 
                }
                for(int i=0; i<4; i++){
                    if(board[y+i][board[0].length -1 -(y+x+i)]!=currentPlayer)
                        break; 
                    if(i==3)
                        return true; 
                }
            }
        }

        for(int x=0; x<board.length-3; x++){
            for(int y=0; y+x<board.length-3 && y<board[0].length-3; y++ ){
                for(int i=0; i<4; i++){
                    if(board[y+x+i][y+i]!=currentPlayer)
                        break; 
                    if(i==3)
                        return true; 
                }

                for(int i=0; i<4; i++){
                    if(board[y+x+i][board[0].length -1 -(y+i)]!=currentPlayer)
                        break; 
                    if(i==3)
                        return true; 
                }
            }
        }

        return false; 

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
