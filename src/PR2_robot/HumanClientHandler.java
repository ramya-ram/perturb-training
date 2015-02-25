package PR2_robot;

import java.io.IOException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Handles communication with the human through google web speech recognition (on localhost:8080/test)
 */
@WebSocket
public class HumanClientHandler {
	public static String message;
	private static Session session;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
    	HumanClientHandler.session = session;
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
        try {
        	HumanClientHandler.session.getRemote().sendString("Connected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When a message is received, it is saved as a variable to be used by another class/function
     */
    @OnWebSocketMessage
    public void onMessage(String message) {
    	HumanClientHandler.message = message;
    	System.out.println("Message from human: " + message);
    }
    
    /**
     * Sends the given message to Google Web Speech Recognition through a web socket
     */
    public static void sendMessage(String msg) {
        try {
        	System.out.println("Message to human: "+msg);
        	HumanClientHandler.session.getRemote().sendString(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Resets to no message to indicate that the human has not yet responded
     */
    public static void resetMessage() {
    	HumanClientHandler.message = "";
    }
}