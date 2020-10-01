/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package istcheckers;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;

/**
 *
 * @author 213120
 */
public class Checker{
    private final static int dim = 75;
    public CheckerType checkerType;
    
    public Checker(CheckerType checkerType) {
        this.checkerType = checkerType;
    }

    
    public void erase(Graphics g) {
        g.dispose();
    }
    
    public void draw(Graphics g, int col, int row) {
        int x = col - dim/2;
        int y = row - dim/2;
        //Set Colour
        g.setColor(checkerType == CheckerType.BLACK_REGULAR || checkerType == CheckerType.BLACK_KING ? Color.BLACK : Color.WHITE);
        //Draw
        g.fillOval(x, y, dim, dim);
        g.setColor(Color.WHITE);
        g.drawOval(x, y, dim, dim);

      if (checkerType == CheckerType.WHITE_KING || checkerType == CheckerType.BLACK_KING) {
          g.setColor(Color.decode("#B29700"));
          //g.fillOval(x + dim/4, y + dim/4, dim/2, dim/2);
          g.fillRoundRect(x+dim/4, y+dim/4, dim/2, dim/2, dim/4, dim/4);
           //g.drawString("K", col, row);
      }
      
    }
    
    public static int getDimension()
   {
      return dim;
   }
    
    public static boolean contains(int x, int y, int col, int row)
   {
      return (col - x) * (col - x) + (row - y) * (row - y) < dim / 2 * dim / 2;
   }
    
}
