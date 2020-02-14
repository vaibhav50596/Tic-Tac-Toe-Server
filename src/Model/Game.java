package Model;

import java.io.*;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class Game implements Constants, Runnable {

    private PlayerHelper xPlayerHelper, oPlayerHelper;
    private Board theBoard;
    private ObjectOutputStream outputStreamX, outputStreamO;
    private ObjectInputStream inputStreamX, inputStreamO;

    public Game(ObjectOutputStream outputStreamX,  ObjectOutputStream outputStreamO,
                ObjectInputStream inputStreamX, ObjectInputStream inputStreamO){
        theBoard = new Board();
        this.outputStreamX = outputStreamX;
        this.outputStreamO = outputStreamO;
        this.inputStreamX = inputStreamX;
        this.inputStreamO = inputStreamO;
        }

    public void setupTheGame() {
        xPlayerHelper = new PlayerHelper(new Player("", 'Z'), null, null, 0);
        oPlayerHelper = new PlayerHelper(new Player("", 'Z'), null, null, 0);
        sendObjectX(13, "");
        sendObjectO(13, "");
        try {
            xPlayerHelper = (PlayerHelper) inputStreamX.readObject();
            oPlayerHelper = (PlayerHelper) inputStreamO.readObject();
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        xPlayerHelper.getPlayer().setBoard(theBoard);
        oPlayerHelper.getPlayer().setBoard(theBoard);
        xPlayerHelper.getPlayer().setOpponent(oPlayerHelper.getPlayer().getOpponent());
        oPlayerHelper.getPlayer().setOpponent(xPlayerHelper.getPlayer().getOpponent());
        xPlayerHelper.getPlayer().setMark(LETTER_X);
        oPlayerHelper.getPlayer().setMark(LETTER_O);
        startTheGame();
    }

    public void startTheGame() {
        sendObjectX(1, "Two players have joined the game. Game has started");
        sendObjectO(1, "Two players have joined the game. Game has started");
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendObjectO(2, "");
        sendObjectX(2, "");
        makeMove();
    }
    public void makeMove() {
        while (true) {
            try {
                if (xMove()) {
                }
                if (checkMove()) {
                    break;
                }
                if (oMove()) {
                }
                if (checkMove()) {
                    break;
                }
            }
            catch (SocketException e) {
                closeStream();
            }
            catch (IOException e) {
                closeStream();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean xMove() throws IOException, ClassNotFoundException {
        while(true) {
            sendObjectXO(3, xPlayerHelper.getPlayer().getName() + " your turn",
                    4, "Please wait X is making the move");
            xPlayerHelper = (PlayerHelper) inputStreamX.readObject();

            if (validateMove(xPlayerHelper)) {  // it to will check if the mark can be made other it is invalid
                sendObjectX(6, "Please Wait, O is making the move");
                theBoard.addMark(xPlayerHelper.getPosition()[0], xPlayerHelper.getPosition()[1], xPlayerHelper.getPlayer().getMark());

                oPlayerHelper.setPosition(xPlayerHelper.getPosition());
                sendObjectO(10, "");
                break;
            }
            else { // now tell the client your mark is not valid make another move
                sendObjectX(7, "Invalid Move " + xPlayerHelper.getPlayer().getName() + ", your turn again");
            }
        }
        return true;
    }

    public boolean oMove() throws IOException, ClassNotFoundException {
        while (true) {
            sendObjectXO(4, "Please wait O is making the move",
                    3, oPlayerHelper.getPlayer().getName() + " your turn");
            oPlayerHelper = (PlayerHelper) inputStreamO.readObject();

            if (validateMove(oPlayerHelper)) { // it to will check if the mark can be made other it is invalid
                sendObjectO(6, "Please Wait, X is making the move");
                theBoard.addMark(oPlayerHelper.getPosition()[0], oPlayerHelper.getPosition()[1], oPlayerHelper.getPlayer().getMark());

                xPlayerHelper.setPosition(oPlayerHelper.getPosition());
                sendObjectX(11, "");
                break;
            }
            else { // now tell the client your mark is not valid make another move
                sendObjectX(7, "Invalid Move " + oPlayerHelper.getPlayer().getName() + ", your turn again");
            }
        }
        return true;
    }

    public boolean checkMove() {
        if (theBoard.xWins()) { // Returns True if there is any tie
            sendObjectXO(9, xPlayerHelper.getPlayer().getName() +" you are the Winner!",
                    12,oPlayerHelper.getPlayer().getName() +" you lost the Game!");
            return true;
        }
        else if(theBoard.oWins()) {
            sendObjectXO(12, xPlayerHelper.getPlayer().getName() +" you lost the Game!",
                    9, oPlayerHelper.getPlayer().getName() +" you are the Winner!");
            return true;
        }
        else if(theBoard.isFull()) {
            sendObjectXO(8, "The game has ended. It's a tie!",
                    8, "The game has ended. It's a tie!");
            return true;
        }
        return false;
    }

    public boolean validateMove(PlayerHelper aPlayerHelper) {
        if(theBoard.getMark(aPlayerHelper.getPosition()[0],aPlayerHelper.getPosition()[1]) == SPACE_CHAR)
            return true;
        return false;
    }

    public void sendObject(ObjectOutputStream outputStreamA, PlayerHelper aPlayerHelper, int responseNumber, String message) {
        aPlayerHelper = new PlayerHelper(aPlayerHelper.getPlayer(), aPlayerHelper.getPosition(), null, 0);
        aPlayerHelper.setResponseNumber(responseNumber);
        aPlayerHelper.setLine(message);
        try {
            outputStreamA.writeObject(aPlayerHelper);
        } catch (SocketException e) {
            System.err.println("socket exception line 163");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.err.println("error sending object line 167");
            e.printStackTrace();
        }
    }

    public void sendObjectX(int responseNumber, String message) {
        sendObject(outputStreamX, xPlayerHelper, responseNumber, message);
    }

    public void sendObjectO(int responseNumber, String message) {
        sendObject(outputStreamO, oPlayerHelper, responseNumber, message);
    }

    public void sendObjectXO(int xResponseNumber, String xMessage, int oResponseNumber, String oMessage) {
        sendObjectX(xResponseNumber, xMessage);
        sendObjectO(oResponseNumber, oMessage);
    }

    public void closeStream() {
        try {
            inputStreamO.close();
            inputStreamX.close();
            outputStreamO.close();
            outputStreamX.close();
        } catch (SocketException e) {
            System.err.println("Someone closed the game, please check the Sockets");
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {
                setupTheGame();
                break;
        }
    }
}
