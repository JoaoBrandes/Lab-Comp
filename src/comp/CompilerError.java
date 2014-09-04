package comp;
import java.io.*;

import lexer.*;

public class CompilerError {


    public CompilerError( PrintWriter out ) {
          // output of an error is done in out
        this.out = out;
        thereWasAnError = false;
    }


    public void setLexer( Lexer lexer ) {
        this.lexer = lexer;
    }


    public boolean wasAnErrorSignalled() {
        return thereWasAnError;
    }


    public void show( String strMessage ) {
        show( strMessage, false);
    }


    public void show( String strMessage, boolean goPreviousToken ) {
        // is goPreviousToken is true, the error is signalled at the line of the
        // previous token, not the last one.
        if ( goPreviousToken )
           show( strMessage, lexer.getLineBeforeLastToken(),
                 lexer.getLineNumberBeforeLastToken() );
        else
           show( strMessage, lexer.getCurrentLine(), lexer.getLineNumber() );
    }


   public void show( String strMessage, String lineWithError, int lineNumber ) {
      String msg = lineNumber + " : " + strMessage;
      out.println(msg);
      out.println(lineWithError);
      System.out.println( msg );
      System.out.println( lineWithError );
      out.flush();
      if ( out.checkError() )
         System.out.println("Error in signaling an error");
      thereWasAnError = true;
      throw new RuntimeException();
   }

   public void showWarning( String strMessage ) {
      String msg = "Warning : " + strMessage;
      out.println( msg );
      System.out.println(msg);
      out.flush();
      if ( out.checkError() )
         System.out.println("Error in signaling an error");
      thereWasAnError = true;
      throw new RuntimeException();
   }


   public void show( int messageNumber ) {
      if ( messageNumber < 0 || messageNumber >= last_error )
         show("Internal error: unidentified error number");
      else {
         if ( messageNumber == semicolon_expected )
            show(strError[messageNumber], true);
         else
            show(strError[messageNumber], false);
      }
   }


    public final static int
       ident_expected = 0,
       semicolon_expected = 1,
       last_error = 2;
    public final static String strError[] = {
       "Identifier expected",
       "; expected",
    };
    private Lexer lexer;
    private PrintWriter out;
    private boolean thereWasAnError;
}

/*
begin expected

*/