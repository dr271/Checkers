/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package istcheckers;

/**
 *
 * @author 213120
 */
public class Move {
    public int startX;
    public int startY;
    public int endX;
    public int endY;
    //public boolean isCap;
    
    public Move(int x, int y, int X, int Y) {
        this.startX = x;
        this.startY = y;
        this.endX = X;
        this.endY = Y;
        //isCap = cap;
    }
    
    public int getStartX() {
        return startX;
    }
    
    public int getStartY() {
        return startY;
    }
    
    public int getEndX() {
        return endX;
    }
    
    public int getEndY() {
        return endY;
    }
    
//    public boolean isCapMove() {
//        return isCap;
//    }
}
