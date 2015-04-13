package PR2_robot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.Timer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import code.*;

/**
 * Controls the server for the game, the human and robot connect as two clients and the server handles turn taking within the game
 */
public class MyServer {
    private ServerSocket regularSocket;
    private Server webServer;
    private RobotClientHandler robotHandler;
    PrintWriter out = null;
    InputStreamReader inStream = null;
    BufferedReader in;
    static String lastMessageSent = "";
    static Timer timer;
    static int NUM_SECS_RESEND = 15;
    static int resendTimeLeft = NUM_SECS_RESEND; 

    /**
     * Connects to both the human (through Google Web Speech Recognition) and robot (through ROS) clients
     */
    public MyServer() {
        try {
        	timer = new Timer(1000, timerListener());
            regularSocket = new ServerSocket(7777);
            webServer = new Server(new InetSocketAddress("127.0.0.1", 80));
            WebSocketHandler wsHandler = new WebSocketHandler() {
	            @Override
	            public void configure(WebSocketServletFactory factory) {
	                factory.register(HumanClientHandler.class);
	            }
	        };
	        
            webServer.setHandler(wsHandler);
	        webServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a message to the client specified by the 'client' parameter
     */
    public void sendMessage(String msg, int client) throws Exception{
    	lastMessageSent = new String(msg);
    	if(client == Constants.ROBOT){
    		robotHandler.sendMessage(msg);
    	} else {
    		HumanClientHandler.sendMessage(msg);
    	}
    }
    
    /**
     * Parses human input received from Google Web Speech Recognition
     * Looks for keywords to determine communication type and actions
     * Prompts experimenter if these keywords are not found or are not sufficient to determine what the human wants to do
     */
    public CommResponse parseHumanInput(String str, Action suggestedHumanAction){
    	str = str.toLowerCase();
    	CommType type = null;
    	Action humanAction = null;
    	Action robotAction = null;
    	if(LearningAlgorithm.currCommunicator == Constants.ROBOT){
	    	if(str.contains("yes") || str.contains("yeah") || str.contains("accept"))
	    		type = CommType.ACCEPT;
	    	else if(str.contains("no") || str.contains("instead"))
	    		type = CommType.REJECT;
    	} else if(LearningAlgorithm.currCommunicator == Constants.HUMAN){
	    	if(str.contains("suggest"))
	    		type = CommType.SUGGEST;
	    	else if(str.contains("extinguish"))
	    		type = CommType.UPDATE;
    	}

    	if(type == CommType.ACCEPT){
    		humanAction = suggestedHumanAction;    			
    	}
    	else if(type == CommType.REJECT || type == CommType.UPDATE){
    		humanAction = getActionFromInput(str);
    	} else if(type == CommType.SUGGEST){
    		int index = str.indexOf("suggest");
    		String humanStr = str.substring(0, index);
    		String robotStr = str.substring(index);
    		humanAction = getActionFromInput(humanStr);
    		robotAction = getActionFromInput(robotStr);
    	}
    	System.out.println("type "+type+" humanAction "+humanAction);
    	if(type == null || humanAction == null){
    		System.out.println("What was the type of communication (A = Accept, R = Reject, S = Suggest, U = Update)?");
    		type = getCommType(Tools.scan.next().toUpperCase().trim().charAt(0));
    		if(type == CommType.ACCEPT){
    			humanAction = suggestedHumanAction;
    		} else{
	    		System.out.println("What was the human's action? (A = Alpha, B = Bravo, C = Charlie, D = Delta, E = Echo)");
	    		humanAction = getAction(Tools.scan.next().toUpperCase().charAt(0)); 	
    		}
    	}
    	if(type == CommType.SUGGEST && robotAction == null){
    		System.out.println("What was the suggested robot action? (A = Alpha, B = Bravo, C = Charlie, D = Delta, E = Echo)");
    		robotAction = getAction(Tools.scan.next().toUpperCase().charAt(0));
    	}
    		
    	CommResponse response = new CommResponse(type, humanAction, robotAction);
    	System.out.println(response);
    	return response;
    }
    
    public Action getAction(char c){
    	int index = c - 'A';
    	if(index >= 0 && index < Constants.NUM_PARTS)
    		return Action.valueOf("PUT_OUT"+index);
		return Action.WAIT;
    }
    
    public CommType getCommType(char c){
    	switch(c){
	    	case 'A':
	    		return CommType.ACCEPT;
	    	case 'R':
	    		return CommType.REJECT;
	    	case 'S':
	    		return CommType.SUGGEST;
	    	case 'U':
	    		return CommType.UPDATE;
    	}
    	return null;
    }
    
    public Action getActionFromInput(String str){
    	str.toLowerCase();
    	Action action = null;
    	/*if(str.contains("alpha"))
			action = Action.PUT_OUT0;
		else if(str.contains("bravo"))
			action = Action.PUT_OUT1;
		else if(str.contains("charlie"))
			action = Action.PUT_OUT2;
		else if(str.contains("delta"))
			action = Action.PUT_OUT3;
		else if(str.contains("echo"))
			action = Action.PUT_OUT4;*/
    	return action;
    }
    
    public String getRobotMessage() {
    	try{
    		return robotHandler.getMessage();
    	} catch(Exception e){
    		e.printStackTrace();
    	}
    	return "None";
    }
    
    /**
     * Gets messages from the human and calls parseHumanInput to determine the communication type and actions
     * Keeps track of the time left and forces a wait action if the time goes to 0
     * Resends a message to the client is a response hasn't been received in some amount of time
     */
    public CommResponse getHumanMessage(Action suggestedHumanAction) {
    	try{
			resendTimeLeft = NUM_SECS_RESEND;
			timer.start();
			while(HumanClientHandler.message == null){
				if(resendTimeLeft == 0){
					timer.stop();
					sendMessage(lastMessageSent, Constants.HUMAN);
					resendTimeLeft = NUM_SECS_RESEND;
					timer.start();
				}
				if(LearningAlgorithm.timeLeft == 0){
					System.out.println("time over");
    				return new CommResponse(CommType.NONE, Action.WAIT, Action.WAIT);	
    			}
				System.out.print("");
			}
			System.out.println("before parse input message "+HumanClientHandler.message);
			String temp = HumanClientHandler.message;
			HumanClientHandler.message = null;
			return parseHumanInput(temp, suggestedHumanAction);
        } catch(Exception e){
        	e.printStackTrace();
        }
    	return new CommResponse(CommType.NONE, Action.WAIT, Action.WAIT);	
    }
    
    public void initConnections() {
        System.out.println("Waiting for client...");
        try{     	
	        Socket robotSocket = regularSocket.accept();
	       	robotHandler = new RobotClientHandler(robotSocket);
	       	System.out.println("Connected to "+robotSocket);
        } catch(Exception e){
        	e.printStackTrace();
        }
    }
    
    public ActionListener timerListener() {
		return new ActionListener() {
		  public void actionPerformed(ActionEvent evt) {
			  resendTimeLeft--;
		  }
		};
	}
}