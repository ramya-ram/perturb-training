package code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Used to connect to SocketTest that acts as an interface to the human
 */
public class SocketConnect {
	Socket MyService = null; 
    int portnumber = 256;
    String ipaddress = "127.0.0.1";
    PrintWriter out = null;
    InputStreamReader inStream = null;
    BufferedReader in;
    static BufferedWriter writer;
    
    /*public SocketConnect(){
    	this.ipaddress = ipaddress;
    	this.portnumber = portnumber;
    }*/
    
    /**
     * Sends a message from the server to the client
     */
    public void sendMessage(String msg) throws Exception {	
		out.println(msg);
		out.flush();
		writer.write(msg+"\n");
		System.out.println("Client>" + msg);	
	}
    
    /**
     * Receives a message from the client
     */
    public String getMessage() throws Exception {
    	String message = null;
    	boolean messageReceived = false;
    	try {
    		while(messageReceived == false){	
    			//if time ran out, the human forfeits their turn
    			if(LearningAlgorithm.timeLeft == 0){
    				message = "NONE";
    				messageReceived = true;
    			}
    			if(inStream != null){
    				if(in.ready()){
    					message = in.readLine();
    					messageReceived = true;
    					System.out.println("Server: " + message);
    				}
    			}
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	writer.write(message+"\n");
    	return message;
    }
    
	public void initializeConnection(){	
		try{
			MyService = new Socket(ipaddress, portnumber);
			System.out.println("Initializing connection ");
            out = new PrintWriter(MyService.getOutputStream());
            inStream = new InputStreamReader(
            		MyService.getInputStream());
            in = new BufferedReader(inStream);
            writer = new BufferedWriter(new FileWriter(new File(Constants.socketTestOutputName)));
		}
		catch (IOException e) {
			System.out.println(e);
			System.exit(0);
	    } 
	}
}
