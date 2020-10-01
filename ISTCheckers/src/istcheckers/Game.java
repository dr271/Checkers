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

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;



public class Game extends JFrame{
    public static int maxDepth = 1;
    public static boolean firstTurn = true;
    JMenuBar menuBar;
    
    //Creates new Game instance
    public static void main(String[] args) {
        Runnable r = new Runnable()
                   {
                      @Override
                      public void run()
                      {
                         new Game();
                      }
                   };
      EventQueue.invokeLater(r);
    }
    
    //Constructor for new Game, initialises new Board
    public Game() {
        super("Mega Checkers-3000");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Board board = new Board(); 
        board.resetBoard();
        setContentPane(board);
        setResizable(false);
        createMenu(board);
        
        pack();
        setVisible(true);
    }
    
    //Creates the menu
    public void createMenu(Board board) {
        menuBar = new JMenuBar();
        //Difficulty drop down
        JMenu difficulty = new JMenu("Difficulty");
        
        //Difficulty selection radio buttons
        ButtonGroup difficultyGroup = new ButtonGroup();
        JRadioButtonMenuItem rbEasy = new JRadioButtonMenuItem("Piece of Cake");
        JRadioButtonMenuItem rbMedium = new JRadioButtonMenuItem("Let's Rock");
        JRadioButtonMenuItem rbHard = new JRadioButtonMenuItem("Come Get Some");
        JRadioButtonMenuItem rbVHard = new JRadioButtonMenuItem("Damn I'm good!");
        
        //Action listeners to provide functionality
        rbEasy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetBoard();
                System.out.println("Don't worry, I remember my first time");
                maxDepth = 1;
            }
        });
        rbMedium.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetBoard();
                System.out.println("Getting the hang of this game? I'll still play with one hand tied behind my back");
                maxDepth = 3;
            }
        });
        rbHard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetBoard();
                System.out.println("Let's see what you've got");
                maxDepth = 5;
            }
        });
        rbVHard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetBoard();
                System.out.println("Prepare to be humiliated!");
                maxDepth = 15;
            }
        });
        
        //Group all radio buttons
        difficultyGroup.add(rbEasy);
        difficultyGroup.add(rbMedium);
        difficultyGroup.add(rbHard);
        difficultyGroup.add(rbVHard);
        
        //Ensure correct one is selected
        if(maxDepth == 1) {
            rbEasy.setSelected(true);
            rbMedium.setSelected(false);
            rbHard.setSelected(false);
            rbVHard.setSelected(false);
        } else if (maxDepth == 3) {
            rbEasy.setSelected(false);
            rbMedium.setSelected(true);
            rbHard.setSelected(false);
            rbVHard.setSelected(false);
        } else if (maxDepth == 5) {
            rbEasy.setSelected(false);
            rbMedium.setSelected(false);
            rbHard.setSelected(true);
            rbVHard.setSelected(false);
        } else if (maxDepth == 15) {
            rbEasy.setSelected(false);
            rbMedium.setSelected(false);
            rbHard.setSelected(false);
            rbVHard.setSelected(true);
        }
        //Add the group to difficulty tab
        difficulty.add(rbEasy);
        difficulty.add(rbMedium);
        difficulty.add(rbHard);
        difficulty.add(rbVHard);
        menuBar.add(difficulty);
        
        //Create Options dropdown
        JMenu options = new JMenu("Options");
        menuBar.add(options);
        
        //Add reset Board as option
        JMenuItem resetBoard = new JMenuItem("Reset Board");
        options.add(resetBoard);
        resetBoard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e ) {
                resetBoard();
            }
        });
        
        //Add instructions to options
        JMenuItem howToPlay = new JMenuItem("How To Play");
        options.add(howToPlay);
        howToPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e ) {
                UIManager.put("OptionPane.messageFont", new Font("Arial", Font.PLAIN, 17));
                String html = "<html><body width='%1s'><h1>Rules</h1>"
                + "<ul>"
                
                + "<li>Drag and drop checkers with your mouse to play. </li>"
                + "<br>"
                + "<li>The objective of the game is to create a situation in which it is impossible for your opponent to make any move, usually due to complete elimination. </li>"
                + "<br>"
                + "<li>From their initial positions, checkers may only move diagonally and forwards - therefore they only ever land on the black squares.</li>"
                + "<br>"
                + "<li>A checker can only move one sqaure, unless they are performing a capture move. This is where a checker captures another piece by jumping over it diagonally onto a vacant square.</li>"
                + "<br>"
                + "<li>On a capturing move, a piece may make multiple consecutive jumps. If after a jump the piece is in a position to make another jump then they may do so. This means that a player may make several jumps in succession, capturing several pieces during a single turn.</li>"
                + "<br>"
                + "<li>If a player is in a position to make a capturing move, they must make a capturing move. If multiple are available, it is the players choice as to which one.</li>"
                + "<br>"
                + "<li>When a checker reaches the opponents end of the board, they become a King, signified by the gold crown on the checker.</li>"
                + "<br>"
                + "<li>Kings gains an added ability to move diagonally backwards.</li>"    
                + "</ul>";
                int w = 500;
                JOptionPane.showMessageDialog(null, String.format(html, w, w));
            }
        });
        
        //Add hints to options
        JMenuItem showHints = new JMenuItem("Toggle Hints");
        options.add(showHints);
        showHints.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e ) {
                if (firstTurn) {
                    System.out.println("Have a go first! You can move any on the upper row.");
                } else {
                    board.toggleHints();
                    board.updateGUI();
                } 
            }
        });
        //display the completed menu
        this.setJMenuBar(menuBar);
    }
    
    
    public void resetBoard() {
        System.out.println("Board Reset");
        dispose();
        new Game();
    }
    
    //Getters and Setters
    public static int getMaxDepth() {
        return maxDepth;
    }
    
    public static void infoBox(String infoMessage, String titleBar){
        JOptionPane.showMessageDialog(null, infoMessage, "" + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }
}
