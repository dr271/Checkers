/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package istcheckers;
//Necessary imports
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import static java.lang.Integer.min;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;

/**
 *
 * @author 213120
 */
public class Board extends JComponent{
    private final static int TileDim = (int) (Checker.getDimension() * 1.25); //Tile dimension, 25% larger than a checker
    private final int BoardDim = 8 * TileDim;
    private Dimension dimSize;
    //Click + Drag support
    private boolean beingDragged = false;
    private int disX, disY;                         //distance between checker centre + start of drarg
    private CheckerPos checkerPos;                  //start of drag checker pos
    private int oldX, oldY;                         // centre loc of above
    private List<CheckerPos> checkerPoss;           //Array list checker rep for GUI
    List<CheckerPos> toRemove = new ArrayList<CheckerPos>();
    private List<Move> moves = new ArrayList<>();   //possible non capturing moves
    private List<Move> movesCap = new ArrayList<>(); //possible capturing moves
    private List<Move> allValidMoves = new ArrayList<>(); //all possible moves
    //private List<MovesAndScores> successorEvaluations; //List of possible moves with associated scores
    public boolean occupied[][] = new boolean[80][80];
    private char boardRep[][];                      //Main board representation
    public int maxDepth = Game.getMaxDepth();       //Tree search depth - dictates difficulty
    int bestOldX, bestOldY, bestNewX, bestNewY;     //Components of selected best move
    private boolean hintsDisplayed = false;
    boolean playerTurn = true;                      //Tracks who's turn it is
    boolean capturePerformed = false;               //Used to track when a second turn should be allowed
    boolean AIextraCapMove = false;                 //Tracks when AI is allowed a second turn
    boolean regicide = false;                       //Used to handle promotion when king is captured by normal checker
    
    
    public Board() {
        checkerPoss = new ArrayList<>();
        dimSize = new Dimension(BoardDim, BoardDim);
        
        // x = Red tiles, can't be used at any time
        // o = Available black tile
        // w = White Checker, b = Black Checker
        // W = White king, B = Black king
        boardRep = new char[][] {
            {'x', 'w', 'x', 'w', 'x', 'w', 'x', 'w'},
            {'w', 'x', 'w', 'x', 'w', 'x', 'w', 'x'},
            {'x', 'w', 'x', 'w', 'x', 'w', 'x', 'w'},
            {'o', 'x', 'o', 'x', 'o', 'x', 'o', 'x'},
            {'x', 'o', 'x', 'o', 'x', 'o', 'x', 'o'},
            {'b', 'x', 'b', 'x', 'b', 'x', 'b', 'x'},
            {'x', 'b', 'x', 'b', 'x', 'b', 'x', 'b'},
            {'b', 'x', 'b', 'x', 'b', 'x', 'b', 'x'},
        };
        
        addMouseListener(new MouseAdapter()
                       {
                          @Override
                          public void mousePressed(MouseEvent me)
                          { //Initial Mouse co-ords when clicked
                             int x = me.getX();
                             int y = me.getY();

                             //Find black checker at those co-ords
                             for (CheckerPos checkerPos: checkerPoss)
                                if (Checker.contains(x, y, checkerPos.x, checkerPos.y) && ((checkerPos.checker.checkerType == CheckerType.BLACK_REGULAR || checkerPos.checker.checkerType == CheckerType.BLACK_KING)))
                                {
                                   Board.this.checkerPos = checkerPos;
                                   oldX = checkerPos.x;
                                   oldY = checkerPos.y;
                                   disX = x - checkerPos.x;
                                   disY = y - checkerPos.y;
                                   beingDragged = true;
                                   return;
                                }
                          }

                          @Override
                          public void mouseReleased(MouseEvent me)
                          { 
                             //Update variable
                             if (beingDragged)
                                beingDragged = false;
                             else
                                return;
                             //Snap checker to tile centre
                             int x = me.getX();
                             int y = me.getY();
                             checkerPos.x = (x - disX) / TileDim * TileDim + 
                                           TileDim / 2;
                             checkerPos.y = (y - disY) / TileDim * TileDim + 
                                           TileDim / 2;
                             
                             //Check move is valid
                             if (!validMove(oldX/TileDim, oldY/TileDim, checkerPos.x/TileDim, checkerPos.y/TileDim, boardRep, true)) {
                                 Board.this.checkerPos.x = oldX;
                                 Board.this.checkerPos.y = oldY;
                             } else {
                                 //pause(); //Attempt to pause gui between turns, kinda works (not very well), gives clunky feel to gameplay
                                 Game.firstTurn = false;
                                 playTurn();
                                 
                                 //Handles the AI's consecutive capture moves, without each having to be triggered by mouse release
                                 if (AIextraCapMove) {
                                    minimax(boardRep, 0, "white", Integer.MIN_VALUE, Integer.MAX_VALUE);
                                    //System.out.println(Arrays.deepToString(boardRep));
                                    updateBoardRep(bestOldX*TileDim, bestOldY*TileDim, bestNewX*TileDim, bestNewY*TileDim);
                                    checkForKing(boardRep);
                                    updateGUI();
                                    repaint();
                                    AIextraCapMove = false;
                                    playerTurn = true;
                                 }

                                 //Console feedback
                                 //System.out.println("Valid black moves: " + getAllValidMoves("black", boardRep).size());
                                 //System.out.println("Valid white moves: " + getAllValidMoves("white", boardRep).size());                                
                                 System.out.println("Chosen AI Move: " + (bestOldX + 1) + "," + (bestOldY + 1) + " to " + (bestNewX + 1) + "," + (bestNewY + 1)); //Adjust from 0 indexing to 1
                                 
                                 //Winning conditions
                                 if (blackHasWon()) {
                                     Game.infoBox("BLACK WINS - We'll crank up the difficulty for next time!", "Game Over!");
                                 }
                                 if (whiteHasWon()) {
                                     Game.infoBox("WHITE WINS - Maybe stick to TicTacToe", "Game Over!");
                                 }
                                 
                             }
                             checkerPos = null;
                             repaint();
                          }
                       });
        addMouseMotionListener(new MouseMotionAdapter()
                             {
                                @Override
                                public void mouseDragged(MouseEvent me)
                                {
                                   if (beingDragged)
                                   {
                                      // Update checker ventre loc
                                      checkerPos.x = me.getX() - disX;
                                      checkerPos.y = me.getY() - disY;
                                      repaint();
                                   }
                                }
                             });  
    }
    
    //Attempt to pause play + update GUI between turns
    //Pauses before snapping moved checker to centre, and failed to use it to update GUI between multicap moves
    public void pause() {
        //new Thread(() -> {
            System.out.println("Dramatic paaaauuuuuuse....");
            try {
                //resume = false;
                Thread.sleep(1000);
            } catch (InterruptedException e){
                
                updateGUI();
                repaint();
            }
            //resume= true;
        //}).start();
    }
    
    //Controls who should play when - responsible for allowing multicap turns
    public void playTurn(){
        int newX = checkerPos.x;
        int newY = checkerPos.y;
        int capX = ((oldX + newX)/ 2);
        int capY = ((oldY + newY)/2);
        
        //Apply user turn
        if (playerTurn) { 
            updateBoardRep(oldX, oldY, checkerPos.x, checkerPos.y);
            checkForKing(boardRep);

            //If capturing checker is able to capture another, another turn is allowed
            if (capturePerformed && captureMoveAt(boardRep, checkerPos.x/TileDim, checkerPos.y/TileDim)) {
                System.out.println("Keep it going! You can take another checker!");
            } else {
            playerTurn = false;
            }
        }
        
        //Computers Turn        
        if (!playerTurn){
        minimax(boardRep, 0, "white", Integer.MIN_VALUE, Integer.MAX_VALUE);
        updateBoardRep(bestOldX*TileDim, bestOldY*TileDim, bestNewX*TileDim, bestNewY*TileDim);
        checkForKing(boardRep);
        //If capturing checker is able to capture another, another turn is allowed
            if (capturePerformed && captureMoveAt(boardRep, bestNewX, bestNewY)) {
                System.out.println("ANOTHER GO TO COMPUTER");
                updateGUI();
                repaint();
                //pause();
                AIextraCapMove = true;
                //Thread.sleep(1000);
            } else {
                AIextraCapMove = false;
                playerTurn = true;
            }
        }
        //UI + book keeping needed regardless of who's turn it is
        capturePerformed = false;
        updateGUI();
        repaint();
        //System.out.println(Arrays.deepToString(boardRep));
        regicide = false;
    }
    
    //Performs a capturing move
    public void capture(char[][] board, int oldX, int oldY, int newX, int newY) {
        int capX = ((oldX + newX)/ 2);
        int capY = ((oldY + newY)/2);
        Iterator<CheckerPos> iter = checkerPoss.iterator();
        
        //If a capturing move has been made
        if (Math.abs(newX - oldX) == 2 * TileDim && Math.abs(newY - oldY) == 2 * TileDim) {
            System.out.println("You're mine! at " + capX/TileDim + "," + capY/TileDim);
            capturePerformed = true;
            
            //Regicide - Promote piece to king if it caps another king          
            if (board[capY/TileDim][capX/TileDim] == 'W') {
                regicide = true;
                //System.out.println("KING CAPPED");
                boardRep[newY/TileDim][newX/TileDim] = 'B';

                for (CheckerPos checkerPos: checkerPoss) {
                    if (checkerPos.y == newY && checkerPos.x == newX) {
                        System.out.println("CHECKERPOS UPDATED to king");
                        checkerPos.checker.checkerType = CheckerType.BLACK_KING;
                        repaint();
                    }
                } 
            }
            //Regicide for the AI (white) Team
            if (board[capY/TileDim][capX/TileDim] == 'B') {
                regicide = true;
                System.out.println("KING CAPPED");
                boardRep[newY/TileDim][newX/TileDim] = 'W';
                //System.out.println("cap: " + Arrays.deepToString(boardRep));
                //Promote correct checkerPos type to king
                for (CheckerPos checkerPos: checkerPoss) {
                    if (checkerPos.y == newY && checkerPos.x == newX) {
                        //System.out.println("CHECKERPOS UPDATED to king");
                        checkerPos.checker.checkerType = CheckerType.WHITE_KING;
                        repaint();
                    }
                } 
            }

            //Update Board Rep + GUI
            board[capY/TileDim][capX/TileDim] = 'o'; 
            for (CheckerPos checkerPos: checkerPoss) {
                if (checkerPos.x == capX && checkerPos.y == capY) {
                    checkerPoss.remove(checkerPos);
                    //toRemove.add(checkerPos);
                    updateGUI();
                    repaint();
                    break;
                }
            }
        } 
    }
    
    //Promotes checkers who have reached opposing side of board
    public void checkForKing(char[][] board) {
        for (int x = 0; x < 8; x++) {
            if (board[0][x] == 'b') {
                board[0][x] = 'B';
                for (CheckerPos checkerPos: checkerPoss) {
                    if (checkerPos.y == TileDim /2 && checkerPos.checker.checkerType == CheckerType.BLACK_REGULAR) {
                        checkerPos.checker.checkerType = CheckerType.BLACK_KING;
                        repaint();
                    }
                }
            }
            if (boardRep[7][x] == 'w') {
                boardRep[7][x] = 'W';
                //System.out.println("REP UPDATED");
                for (CheckerPos checkerPos: checkerPoss) {
                    if (checkerPos.y == BoardDim - TileDim/2 && checkerPos.checker.checkerType == CheckerType.WHITE_REGULAR) {
                        checkerPos.checker.checkerType = CheckerType.WHITE_KING;
                        repaint();
                    }
                }
            }
        }
    }
    
    //Dictate whether the described move is valid
    public boolean validMove(int x, int y, int X, int Y, char[][] board, boolean errMsg) {
        boolean valid = true;
        int oldX = x;
        int oldY = y;
        int newX = X;
        int newY = Y;
        
        //Capture moves must be made if available
        if(board[oldY][oldX] == 'b' || board[oldY][oldX] == 'B') {
            if (existsCapMove(board, "black") && !captureMovePerformed(board, oldX, oldY, newX, newY)) {
                if (errMsg) System.out.println("If you can capture a piece, you must!");
                valid = false;
            }
        }
        if(board[oldY][oldX] == 'w' || board[oldY][oldX] == 'W') {
            //System.out.println("MOVING A WHITE PIECE");
            if (existsCapMove(board, "white") && !captureMovePerformed(board, oldX, oldY, newX, newY)) {
                if (errMsg) System.out.println("If you can capture a piece, you must!");
                valid = false;
            }
        }
        
        //Prevents moving out of bounds
        if (newX > 7 || newX <0 || newY > 7 || newY < 0) {
            if (errMsg) System.out.println("Are you trying to run away? That's out of bounds!");
            return false;
        }
         //Checks tile is not already occupied
        if (board[newY][newX] == 'b' || board[newY][newX] == 'w' || board[newY][newX] == 'B' || board[newY][newX] == 'W') {
            valid = false;
            if (errMsg) System.out.println("Invalid move! This square ain't big enough for the both of us!");
        }
         //Prevent moving in a straight line
        if (newX == oldX || newY == oldY) {
            valid = false;
            if (errMsg) System.out.println("Nope! You gotta move diagonally!");
        }
        //Only allow moving forwards
        if (board[oldY][oldX] == 'b' && newY > oldY){
            valid = false;
            if (errMsg) System.out.println("You can only move forwards!");
        }
        if (board[oldY][oldX] == 'w' && newY < oldY){
            valid = false;
            if (errMsg) System.out.println("You can only move forwards!");
        }
        //Forbid moving more than one space away`
        if ((Math.abs(newX - oldX) > 1 || Math.abs(newY - oldY) > 1) && !captureMoveAt(board, oldX, oldY)){ 
            valid = false;
            if (errMsg) System.out.println("Woah slow down there buddy! You can't move more than 1 square away!");
        } else if (Math.abs(newX - oldX) > 2 || Math.abs(newY - oldY) > 2) {
            valid = false;
            if (errMsg) System.out.println("Woah slow down there buddy! You can't move more than 2 squares away when taking a piece!");
        }
        return valid;
    }
    
    //Creates an arrayList of all valid Moves for a given team and board representation
    public List<Move> getAllValidMoves(String team, char[][] board) {
        movesCap.clear();
        moves.clear();
        allValidMoves.clear();
        //Adds all capturing moves
        for (int i = 0; i<8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((team == "white" && (board[i][j] == 'w' || board[i][j] == 'W')) || (team == "black" && (board[i][j] == 'b' || board[i][j] == 'B'))) {
                    if (captureMovePerformed(board, j, i, j + 2, i + 2)) {
                            movesCap.add(new Move(j, i, j+2, i+2));
                        }
                        if (captureMovePerformed(board, j, i, j - 2, i + 2)) {
                            movesCap.add(new Move(j, i, j-2, i+2));
                        }
                        if (captureMovePerformed(board, j, i, j + 2, i - 2)) {
                            movesCap.add(new Move(j, i, j+2, i-2));
                        }
                        if (captureMovePerformed(board, j, i, j - 2, i - 2)) {
                            movesCap.add(new Move(j, i, j-2, i-2));
                        }
                }
            }
        }
        //Adds non capturing moves if no capturing ones exist
        if (movesCap.isEmpty()) {
            for (int i = 0; i<8; i++) {
                for (int j = 0; j < 8; j++) {
                    if ((team == "white" && (board[i][j] == 'w' || board[i][j] == 'W')) || (team == "black" && (board[i][j] == 'b' || board[i][j] == 'B'))) {
                        if (validMove(j, i, j + 1, i + 1, board, false)) {
                            moves.add(new Move(j, i, j+1, i+1));
                        }
                        if (validMove(j, i, j - 1, i + 1, board, false)) {
                            moves.add(new Move(j, i, j-1, i+1));
                        } 
                        if (validMove(j, i, j + 1, i - 1, board, false)) {
                            moves.add(new Move(j, i, j+1, i-1));
                        }
                        if (validMove(j, i, j - 1, i - 1, board, false)) {
                            moves.add(new Move(j, i, j-1, i-1));
                        }
                    }
                }
            }
            //Add to one super-list to ease referencing
            allValidMoves.addAll(moves);
            return moves;
        } else {
            allValidMoves.addAll(movesCap);
            return movesCap;
        }  
    }
    
    
    //Dictates if the move just performed was a capturing move
    public boolean captureMovePerformed(char[][] board, int x, int y, int newX, int newY) {
        boolean capMove = false;
        
        //Black caps to uper left
        try {
        if ((board[y][x] == 'b' || board[y][x] == 'B') && ((board[y-1][x-1] == 'w' || board[y-1][x-1] == 'W') && board[y-2][x-2] == 'o')) {
            if (newX == x-2 && newY == y-2) {
                capMove = true;
            }
        }} catch(ArrayIndexOutOfBoundsException e) {}
        //Black caps to upper right
        try {
        if((board[y][x] == 'b'|| board[y][x] == 'B') && ((board[y-1][x+1] == 'w'|| board[y-1][x+1] == 'W') && board[y-2][x+2] == 'o')) {
            if (newX == x+2 && newY == y-2) {
                capMove = true;
            }
        }} catch(ArrayIndexOutOfBoundsException e) {}
        //White caps to lower left
        try{
        if ((board[y][x] == 'w' || board[y][x] == 'W') && ((board[y+1][x-1] == 'b' || board[y+1][x-1] == 'B') && board[y+2][x-2] == 'o')){
            if (newX == x-2 && newY == y+2) {
                capMove = true;
            }
        }} catch(ArrayIndexOutOfBoundsException e) {}
        //White caps to lower right
        try {
        if ((board[y][x] == 'w' || board[y][x] == 'W') && ((board[y+1][x+1] == 'b' || board[y+1][x+1] == 'B') && board[y+2][x+2] == 'o')) {
            if (newX == x+2 && newY == y+2) {
                capMove = true;
            }
        }} catch(ArrayIndexOutOfBoundsException e) {}
        
        
        //Black king lower left caps
        try {
        if ((board[y][x] == 'B')&& ((board[y+1][x-1] == 'w' || board[y+1][x-1] == 'W') && board[y+2][x-2] == 'o')) {
            if (newX == x-2 && newY == y+2) {
                capMove = true;
            }
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //black king lower right caps
        try {
        if ((board[y][x] == 'B')&& ((board[y+1][x+1] == 'w' || board[y+1][x+1] == 'W') && board[y+2][x+2] == 'o')) {
            if (newX == x+2 && newY == y+2) {
                capMove = true;
            }
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //White king upper left
        try {
        if ((board[y][x] == 'W')&& ((board[y-1][x-1] == 'b' || board[y-1][x-1] == 'B') && board[y-2][x-2] == 'o')) {
            if (newX == x-2 && newY == y-2) {
                capMove = true;
            }
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //White king upper right
        try {
        if ((board[y][x] == 'W')&& ((board[y-1][x+1] == 'b' || board[y-1][x+1] == 'B') && board[y-2][x+2] == 'o')) {
            if (newX == x+2 && newY == y-2) {
                capMove = true;
            }
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //System.out.println("Capture move performed: " + capMove);
        return capMove;
    }
    
    //Dictates if a capture move exists at the given co-ordinates
    public boolean captureMoveAt(char[][] board, int x, int y) {
        boolean capMove = false;

        //Black caps to uper left
        try {
        if ((board[y][x] == 'b' || board[y][x] == 'B') && ((board[y-1][x-1] == 'w' || board[y-1][x-1] == 'W') && board[y-2][x-2] == 'o')) {
            capMove = true;
        }} catch(ArrayIndexOutOfBoundsException e) {}
        //Black caps to upper right
        try {
        if((board[y][x] == 'b'|| board[y][x] == 'B') && ((board[y-1][x+1] == 'w'|| board[y-1][x+1] == 'W') && board[y-2][x+2] == 'o')) {
            capMove = true;
        }} catch(ArrayIndexOutOfBoundsException e) {}
        //White caps to lower left
        try{
        if ((board[y][x] == 'w' || board[y][x] == 'W') && ((board[y+1][x-1] == 'b' || board[y+1][x-1] == 'B') && board[y+2][x-2] == 'o')){
            capMove = true;
        }} catch(ArrayIndexOutOfBoundsException e) {}
        //White caps to lower right
        try {
        if ((board[y][x] == 'w' || board[y][x] == 'W') && ((board[y+1][x+1] == 'b' || board[y+1][x+1] == 'B') && board[y+2][x+2] == 'o')) {
            capMove = true;
        }} catch(ArrayIndexOutOfBoundsException e) {}
        
        
        //Black king lower left caps
        try {
        if ((board[y][x] == 'B')&& ((board[y+1][x-1] == 'w' || board[y+1][x-1] == 'W') && board[y+2][x-2] == 'o')) {
            capMove = true;
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //black king lower right caps
        try {
        if ((board[y][x] == 'B')&& ((board[y+1][x+1] == 'w' || board[y+1][x+1] == 'W') && board[y+2][x+2] == 'o')) {
            capMove = true;
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //White king upper left
        try {
        if ((board[y][x] == 'W')&& ((board[y-1][x-1] == 'b' || board[y-1][x-1] == 'B') && board[y-2][x-2] == 'o')) {
            capMove = true;
        }}catch(ArrayIndexOutOfBoundsException e) {}
        //White king upper right
        try {
        if ((board[y][x] == 'W')&& ((board[y-1][x+1] == 'b' || board[y-1][x+1] == 'B') && board[y-2][x+2] == 'o')) {
            capMove = true;
        }}catch(ArrayIndexOutOfBoundsException e) {}
        
        return capMove;
        }
    
    //Dictates if a capture move exists anywhere on the board for a given team
    public boolean existsCapMove(char[][] board, String team) {
        boolean capMove = false;      
        if (team == "black") {
            for (int i = 0; i<8; i++) {
                for (int j = 0; j<8; j++){
                    if (captureMoveAt(board, i, j) && (board[j][i] == 'b' || board[j][i] == 'B')) {
                        capMove = true;
                    }
                }
            }
        }
        if (team == "white") {
            for (int i = 0; i<8; i++) {
                for (int j = 0; j<8; j++){
                    if (captureMoveAt(board, i, j) && (board[j][i] == 'w' || board[j][i] == 'W')) {
                        capMove = true;
                    }
                }
            }
        }
        //System.out.println("Exist cap move: " + capMove);
        return capMove;
    }
      
   //Updates the underlying board representation
   public void updateBoardRep(int x, int y, int newx, int newy) {
        int oldX = x / TileDim;
        int oldY = y/ TileDim;
        int newX = newx / TileDim;
        int newY = newy / TileDim;
        
        capture(boardRep, x, y, newx, newy);
        
        if (!regicide) {
        boardRep[newY][newX] = boardRep[oldY][oldX];
        }
        boardRep[oldY][oldX] = 'o';
        
        updateGUI();
        repaint();
    }

   //Updates the graphical user interface based on the board rep
   public void updateGUI() {
       checkerPoss.clear();
       repaint();
       
       for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                
                if (boardRep[y-1][x-1] == 'w') {
                   this.add(new Checker(CheckerType.WHITE_REGULAR), y, x);
                }
                if (boardRep[y-1][x-1] == 'W') {
                   this.add(new Checker(CheckerType.WHITE_KING), y, x);
                }
                if (boardRep[y-1][x-1] == 'B') {
                   this.add(new Checker(CheckerType.BLACK_KING), y, x);
                }
                if (boardRep[y-1][x-1] == 'b') {
                   this.add(new Checker(CheckerType.BLACK_REGULAR), y, x);
                }
                
            }
        } 
       repaint();
   }
   
   //NOT USED - Initial attemp at minimax. Worked to some extent but did not exhibit intelligent gameplay
   public int minimaxAttempt1(char[][] board, int depth, String team, int alpha, int beta) {
        int maxScore = Integer.MIN_VALUE;//team == otherPlayer(team) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        char[][] newBoard = new char[board.length][board.length];
        int oX, oY, nX, nY, newScore;
        
        //A deep copy of the board must be made to asses the score of it after possible moves have been applied
        for (int i = 0; i< board.length; i++) {
            newBoard[i] = Arrays.copyOf(board[i], board[i].length);
        }
        
        //Array list of all the valid moves for a given team and board representation
        getAllValidMoves(team, board);

        //The score for the board at the maximum depth is returned in the deepest layer of recursion
        if (depth == maxDepth) {
            return getHeuristic(board, otherPlayer(team));
        } else {
        //Otherwise all valid moves are applied to ultimately have their potential score assessed 
        for (int i = 0; i < allValidMoves.size(); i++) {
            oX = allValidMoves.get(i).startX;
            oY = allValidMoves.get(i).startY;
            nX = allValidMoves.get(i).endX;
            nY = allValidMoves.get(i).endY;
            
            newBoard = applyMove(newBoard, oX, oY, nX, nY);
            newScore = minimax(newBoard, depth+1, team, alpha, beta);
            //If the move results in a new high-scoring board, the details of this move are recorded
            if (newScore > maxScore){
                maxScore = newScore;
                bestOldX = oX; bestOldY = oY; bestNewX = nX; bestNewY = nY;
            }
            
            //AB pruning
            if (maxScore > beta) {
                if (maxScore >= alpha){
                    break;
                } else {
                    beta = maxScore;
                }
            }
            if (maxScore < alpha) {
                if (maxScore <= beta) {
                    break;
                } else {
                    alpha = maxScore;
                }
            }   
        }  
        }

        return maxScore;
    } 
   
   //Re implementation of minimax. Works significantly better.
   public int minimax(char[][] board, int depth, String team, int alpha, int beta){
       char[][] minimaxBoard = new char[board.length][board.length];
       List<Move> possibleMoves;
       int maxScore, eval;
       maxScore = Integer.MIN_VALUE;
       
       //Create deep copy of board to evaluate potential moves on
        for (int i = 0; i< board.length; i++) {
            minimaxBoard[i] = Arrays.copyOf(board[i], board[i].length);
        }
       
       //Populate list of possible moves
       possibleMoves = getAllValidMoves(team, board);
       
       //Terminating conditions
       if (depth == maxDepth || possibleMoves.isEmpty()){
           return getHeuristic(board, team); //Other player?
       }
       
       if (team == "white") {
           maxScore = Integer.MIN_VALUE;
           for (int m = 0; m < possibleMoves.size(); m++){
               int oX = possibleMoves.get(m).startX;
               int oY = possibleMoves.get(m).startY;
               int nX = possibleMoves.get(m).endX;
               int nY = possibleMoves.get(m).endY;
               
               //Applies first possible move, updating local boardRep
               minimaxBoard = applyMove(minimaxBoard, oX, oY, nX, nY);
               
               //Recursive minimax call, switching player each time
               eval = minimax(minimaxBoard, depth + 1, otherPlayer(team), alpha, beta);
               
               //Log best move found
               if (eval > maxScore) {
                bestOldX = oX; bestOldY = oY; bestNewX = nX; bestNewY = nY;
                maxScore = eval;
               }
               
               //Alpha Beta pruning
               if (maxScore > alpha) {
                   alpha = maxScore;
               }
               if (alpha >= beta) {
                   break;
               }
           }
           return maxScore;
       }
       else {
           maxScore = Integer.MAX_VALUE;
           for (int j=0; j<possibleMoves.size(); j++){
               int oX = possibleMoves.get(j).startX;
               int oY = possibleMoves.get(j).startY;
               int nX = possibleMoves.get(j).endX;
               int nY = possibleMoves.get(j).endY;
               
               //Applies first possible move, updating local boardRep
               minimaxBoard = applyMove(minimaxBoard, oX, oY, nX, nY);
               
               //Recursive minimax call, switching player each time
               eval = minimax(minimaxBoard, depth + 1, otherPlayer(team), alpha, beta);
               maxScore = min(maxScore, eval);
               
               //Alpha Beta pruning
               if (maxScore < beta) {
                   beta = maxScore;
               }
               if (alpha >= beta) {
                   break;
               } 
           }
           return maxScore;
       }
   }
   
   //Moves a piece within the board representation
   public char[][] applyMove(char[][] board, int oX, int oY, int nX, int nY) {

        //System.out.println(board[oY][oX] + " at " + oX + "," + oY + " Moving to " + board[nY][nX] + " at " + nX + "," + nY);
        board[nY][nX] = board[oY][oX];
        board[oY][oX] = 'o';
        
        
        return board;
    }
   
   //This function returns the score of a given board state for the given team
   public int getHeuristic(char[][] board, String maxPlayer) {
        int score = 0;
        checkForKing(board);
        
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (board[y][x] == 'w' && maxPlayer == "white") {
                    score++;
                } else if (board[y][x] == 'w' && maxPlayer == "black") {
                    score--;
                }
                if(board[y][x] == 'W' && maxPlayer == "white") {
                    score = score + 3;
                } else if (board[y][x] == 'W' && maxPlayer == "black") {
                    score = score - 3;
                }
                if(board[y][x] == 'b' && maxPlayer == "white") {
                    score--;
                } else if (board[y][x] == 'b' && maxPlayer == "black") {
                    score++;
                }
                if(board[y][x] == 'B' && maxPlayer == "white") {
                    score = score - 3;
                } else if (board[y][x] == 'B' && maxPlayer == "black") {
                    score = score + 3;
                }
            }
            
        }

        return score;
    }
   
   //Terminating Conditions
   public boolean blackHasWon() {
        boolean won = false;
        if (getAllValidMoves("white", boardRep).isEmpty()) {
            won = true;
        }
        return won;
    }
    public boolean whiteHasWon() {
        boolean won = false;
        if (getAllValidMoves("black", boardRep).isEmpty()) {
            won = true;
        }
        return won;
    }
   
    //Called by minimax to alternate between MIN and MAXimising player
    public String otherPlayer(String player) {
        String newPlayer;
        if (player == "white") {
            newPlayer = "black";
        } else {
            newPlayer = "white";
        }
        return newPlayer;
    }
    
    //Does exactly what it says on the tin
    public void resetBoard() {
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                if ((x + y)%2 == 1 && x <= 3) { //Only place white checkers on black tiles on the top 3 rows
                    this.add(new Checker(CheckerType.WHITE_REGULAR), x, y);
                } 
                if ((x + y)%2 == 1 && x >= 6) { //Only place black checkers on black tiles on the bottom 3 rows
                    this.add(new Checker(CheckerType.BLACK_REGULAR), x, y);
                }
            }
        }
    }
    
    //Adds a new checker, used for setting up the board.
    public void add(Checker checker, int y, int x) {
        if (x < 1 || x > 8 || y < 1 || y > 8)
            throw new IllegalArgumentException("Co-ords out of range!");
        CheckerPos checkerPos = new CheckerPos();
        checkerPos.checker = checker;
        checkerPos.x = (x - 1) * TileDim + TileDim /2;
        checkerPos.y = (y - 1) * TileDim + TileDim /2;
        for (CheckerPos _checkerPos: checkerPoss) {
            _checkerPos.Lcap = false;
            _checkerPos.Rcap = false;
            if (checkerPos.x == _checkerPos.x && checkerPos.y == _checkerPos.y)
                throw new AlreadyOccupiedException("(" + y + "," + x + ") is already taken!");
        }
        checkerPoss.add(checkerPos);
    }
    
    @Override
    protected void paintComponent(Graphics g)
   {
      paintBoard(g);
      for (CheckerPos checkerPos: checkerPoss)
         if (checkerPos != Board.this.checkerPos)
            checkerPos.checker.draw(g, checkerPos.x, checkerPos.y);

      if (checkerPos != null)
         checkerPos.checker.draw(g, checkerPos.x, checkerPos.y);
   }
    
    //Draws the checker board
    private void paintBoard(Graphics g){
      ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      for (int row = 0; row < 8; row++){
         g.setColor(((row & 1) != 0) ? Color.BLACK : Color.decode("#FF3333"));
         for (int col = 0; col < 8; col++){
            g.fillRect(col * TileDim, row * TileDim, TileDim, TileDim);
            g.setColor((g.getColor() == Color.BLACK) ? Color.decode("#FF3333") : Color.BLACK);
         }
      }
      //If hints are enabled, all tiles containing a movable checker are filled green
      if(hintsDisplayed){
            if (allValidMoves.size() > 0){
                g.setColor(Color.GREEN);
                for (int m = 0; m < allValidMoves.size(); m++){
                    g.fillRect(allValidMoves.get(m).startX * TileDim, allValidMoves.get(m).startY * TileDim, TileDim, TileDim);
                }
            }
        }
   }
    
    @Override
   public Dimension getPreferredSize()
   {
      return dimSize;
   }
    
   //NOT USED - alternative method for displaying hints. 
   public void drawHintCircle(Graphics g, int x, int y, int r) {
       x = x -(r/2);
       y = y- (r/2);
       g.setColor(Color.CYAN);
       g.fillOval(x,y,r,r);
   }
   
   public void toggleHints(){
       System.out.println("Hints toggled");
       this.hintsDisplayed = !hintsDisplayed;
       updateGUI();
       repaint();
   }
   
   public char[][] getBoard() {
       return boardRep;
   }
}
