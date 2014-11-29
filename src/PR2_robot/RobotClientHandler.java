package PR2_robot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    			//System.out.println("in while");
    			//if(LearningAlgorithm.timeLeft == 0){
    			//	message = "NONE";
    			//	messageReceived = true;
    			//}
    			if(inStream != null){
    				//System.out.println("instream != null");
    				if(in.ready()){
    					System.out.print("w");
    					message = in.readLine();
    					//if(message.length() > 0)
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