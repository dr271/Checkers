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
public class AlreadyOccupiedException extends RuntimeException
{
   public AlreadyOccupiedException(String message)
   {
      super(message);
   }
}

