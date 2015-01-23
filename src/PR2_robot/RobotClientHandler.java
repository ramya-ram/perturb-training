package PR2_robot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles communication with the robot, sends action commands to the robot and receives confirmation when the actions are completed
 */
public class RobotClientHandler implements Runnable {
    public Socket socket;
    PrintWriter out = null;
    InputStreamReader inStream = null;
    BufferedReader in;

    public RobotClientHandler(Socket socket) {
        this.socket = socket;
        try{
        	System.out.println("Initializing connection");
	        out = new PrintWriter(socket.getOutputStream());
	        inStream = new InputStreamReader(
	        		socket.getInputStream());
	        in = new BufferedReader(inStream);
        } catch(Exception e){
        	e.printStackTrace();
        }
        Thread t = new Thread(this);
        t.start();
    }
    
    public void run(){
    	Thread t = new Thread(this);
        t.start();
    }
    
    public void sendMessage(String msg) throws Exception {	
		out.println(msg);
		out.flush();
		System.out.println("Message to robot: " + msg);	
	}

    public String getMessage() throws Exception {
    	String message = null;
    	boolean messageReceived = false;
    	try {
    		while(messageReceived == false){
    			if(inStream != null){
    				if(in.ready()){
    					System.out.print("w");
    					message = in.readLine();
    					messageReceived = true;
    					System.out.println("Message from robot: " + message);
    					System.out.println("message");
    				}
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	System.out.println("message from robot "+message);
    	return message;
    }
}