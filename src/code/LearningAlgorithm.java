package code;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import PR2_robot.MyServer;

/**
 * Parent class for QLearner and PolicyReuseLearner
 */
public class LearningAlgorithm {
	protected MyWorld myWorld = null;
	protected MDP mdp;
	
	public MyServer myServer;

	public double[][] robotQValues; 
	public double[][][] jointQValues;

	protected boolean withHuman;
	public static int currCommunicator = Constants.ROBOT;
	
	public int numHumanSuggestions = 0;
	public int numHumanUpdates = 0;
	public int numRobotSuggestions = 0;
	public int numRobotUpdates = 0;
	public int numHumanRejects = 0;
	public int numHumanAccepts = 0;
	public int numRobotRejects = 0;
	public int numRobotAccepts = 0;

	public static Timer timer;
	public static int timeLeft = Constants.MAX_TIME;

	/**
	 * Runs one episode of the task
	 */
	public Tuple<Double, Integer, Long> run(boolean fullyGreedy, int maxSteps){
		boolean reachedGoalState = false;
		State state = myWorld.initialState().clone();
        double rewardPerEpisode = 0;
        int iterations = 0;
        int count = 0;
        long startTime = System.currentTimeMillis();
        try{
	        while (!MyWorld.isGoalState(state) && count < maxSteps) {
	        	HumanRobotActionPair agentActions = null;
				if(withHuman && Main.CURRENT_EXECUTION != Main.SIMULATION && !reachedGoalState)
					agentActions = getAgentActionsCommWithHuman(state, null); //communicates with human to choose action until goal state is reached (and then it's simulated until maxSteps)
				else{
					if(fullyGreedy)
						agentActions = getAgentActionsFullyGreedySimulation(state); //for policy reuse, fully greedy is used
					else
						agentActions = getAgentActionsSimulation(state); //uses e-greedy approach (with probability epsilon, choose a random action) 
				}
				
	            State nextState = myWorld.getNextState(state, agentActions);
	            double reward = myWorld.reward(state, agentActions, nextState);
	            rewardPerEpisode+=reward;
	            saveEpisodeToFile(state, agentActions.getHumanAction(), agentActions.getRobotAction(), nextState, reward);     
	            updateQValues(state, agentActions, nextState, reward);
	           
	           //System.out.println(state.toStringFile()+" "+agentActions+" = "+nextState.toStringFile()+" R: "+reward);

	            state = nextState.clone();
	            count++;
	            
            	/*if(!reachedGoalState){
					if(MyWorld.isGoalState(state)){
						iterations = count;
						reachedGoalState = true;
					}
					if(withHuman && Main.gameView != null){
						Main.gameView.setNextEnable(true);
						Main.gameView.waitForNextClick();
						if(reachedGoalState){
							Main.gameView.initTitleGUI("congrats");
						}
					}
            	}*/
	        }
        } catch(Exception e){
        	e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        return new Tuple<Double,Integer,Long>(rewardPerEpisode, iterations, (endTime - startTime));
	}
	
	/**
	 * Updates the joint QValues and the robot's QValues based on an interaction
	 */
	public void updateQValues(State state, HumanRobotActionPair agentActions, State nextState, double reward){
		int humanAction = agentActions.getHumanAction().ordinal();
		int robotAction = agentActions.getRobotAction().ordinal();
		int stateId = state.getId();
		
		double robotQ = getRobotQValue(state, agentActions.getRobotAction());
		double robotMaxQ = maxRobotQ(nextState);	 
        double robotValue = (1 - Constants.ALPHA) * robotQ + Constants.ALPHA * (reward + Constants.GAMMA * robotMaxQ);
        robotQValues[stateId][robotAction] = robotValue;
        
        double jointQ = getJointQValue(state, agentActions);
		double jointMaxQ = maxJointQ(nextState);
		double jointValue = (1 - Constants.ALPHA) * jointQ + Constants.ALPHA * (reward + Constants.GAMMA * jointMaxQ);
		jointQValues[stateId][humanAction][robotAction] = jointValue;
	}
	
	/**
	 * Simulates interactions with the human independently
	 * Either chooses the best joint action or randomly chooses a joint action (with probability epsilon)
	 */
	public HumanRobotActionPair getAgentActionsSimulation(State state){
		HumanRobotActionPair proposedJointAction = null;
		if(Tools.rand.nextDouble() < Constants.EPSILON){
			Action[] possibleRobotActions = mdp.robotAgent.actions(state);
			Action[] possibleHumanActions = mdp.humanAgent.actions(state); //from robot state because that's what robot sees
	        Action robotAction = possibleRobotActions[Tools.rand.nextInt(possibleRobotActions.length)];
	        Action humanAction = possibleHumanActions[Tools.rand.nextInt(possibleHumanActions.length)];
	        proposedJointAction = new HumanRobotActionPair(humanAction, robotAction);
		} else { // otherwise, choose the best action/the one with the highest q value
			Pair<HumanRobotActionPair, Double> proposed = getGreedyJointAction(state);
			proposedJointAction = proposed.getFirst();
		}
		return proposedJointAction;
	}
	
	/**
	 * Prints out q-values of a particular state, for debugging purposes
	 */
	public void numOfNonZeroQValues(State state, String beforeOrAfter, boolean print){
		if(print){
			System.out.println(beforeOrAfter+" simulation");
			try{
				int count = 0;
				int stateId = state.getId();
				for(int j=0; j<jointQValues[stateId].length; j++){
					for(int k=0; k<jointQValues[stateId][j].length; k++){
						if(jointQValues[stateId][j][k] < 0 || jointQValues[stateId][j][k] > 0){
							count++;
							System.out.println("jointQValues["+state.toStringFile()+" "+stateId+"]["+j+"]["+k+"] = "+jointQValues[stateId][j][k]);
						}
					}
				}
				System.out.println("count of nonzero "+count);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Gets the best greedy joint action (no probability of random actions)
	 */
	public HumanRobotActionPair getAgentActionsFullyGreedySimulation(State state){
		Pair<HumanRobotActionPair, Double> proposed = getGreedyJointAction(state);
		return proposed.getFirst();
	}
	
	/**
	 * Prints to SocketTest to get human input
	 * The human and robot communicate to choose a joint action for this state
	 */
	public HumanRobotActionPair getAgentActionsCommWithHuman(State state, Action pastRobotAction){
		try{
			if(Main.gameView != null){
				Main.gameView.setAnnouncements("");
				Main.gameView.setTeammateText("");
				Main.gameView.updateState(state);
			}
			HumanRobotActionPair actions = null;
			if(currCommunicator == Constants.HUMAN){
				actions = humanComm(state, pastRobotAction);
				currCommunicator = Constants.ROBOT;
			} else if(currCommunicator == Constants.ROBOT){
				actions = robotComm(state, pastRobotAction);
				currCommunicator = Constants.HUMAN;
			}
			timer.stop();
			if(Main.gameView != null){
				Main.gameView.setTime(-1);
				if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN && actions.getRobotAction() != Action.WAIT){
					String msg = Main.myServer.getRobotMessage(); //wait until robot completes the action
					System.out.println("msg from robot: "+msg);
				}
			}
			return actions;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public String getPrintableFromAction(Action action){
		if(action != Action.WAIT){
			int fireIndex = Integer.parseInt(action.name().substring(7, 8));
			return "extinguish "+MyWorld.convertToFireName(fireIndex);
		}
		return "wait";
	}
	
	/**
	 * Robot's turn to initiate communication
	 * If the computed joint action is better than the average value, the robot will suggest a joint action, which the human can accept or reject
	 * Otherwise, the robot will just update the human on its action and the human can choose accordingly
	 */
	public HumanRobotActionPair robotComm(State state, Action pastRobotAction) {
		try{
			HumanRobotActionPair actions;
			double maxJointValue = Integer.MIN_VALUE;
			Action bestHumanActionSuggestion = null;
			Action bestRobotActionSuggestion = null;
			Action bestRobotActionUpdate = null;
			double cumulativeValue = 0;
			Action[] humanActions = mdp.humanAgent.actions(state);
			if(pastRobotAction != null){
				System.out.println("pastRobotAction "+pastRobotAction);
				bestRobotActionSuggestion = pastRobotAction;
				bestRobotActionUpdate = pastRobotAction;
				for(Action humanAction : humanActions){
					double value = getJointQValue(state, new HumanRobotActionPair(humanAction, pastRobotAction));
					cumulativeValue += value;
					if(value > maxJointValue){
						maxJointValue = value;
						bestHumanActionSuggestion = humanAction;
					}
				}
			} else {
				System.out.println("not past robotAction "+pastRobotAction);
				Pair<HumanRobotActionPair, Double> pair = getGreedyJointAction(state);
				HumanRobotActionPair agentActions = pair.getFirst();
				maxJointValue = pair.getSecond();
				bestHumanActionSuggestion = agentActions.getHumanAction();
				bestRobotActionSuggestion = agentActions.getRobotAction();
				
				bestRobotActionUpdate = getGreedyRobotAction(state, null);
				//Main.connect.sendMessage("for sugg human "+bestHumanAction+" robot "+bestRobotActionSuggestion+" for update "+bestRobotActionUpdate);
				for(Action humanAction : humanActions){
					double value = getJointQValue(state, new HumanRobotActionPair(humanAction, bestRobotActionUpdate));
					cumulativeValue += value;
				}
			}
			double averageValue = cumulativeValue/humanActions.length;
			
			updateGUIMessage("Waiting for teammate...\n");
			simulateWaitTime(state);
			
			if((maxJointValue - averageValue) > Constants.THRESHOLD_SUGG && bestHumanActionSuggestion != null){ //robot suggests human an action too
				numRobotSuggestions++;
				//enableSend(false);			
				updateGUIMessage("Your teammate will choose to "+getPrintableFromAction(bestRobotActionSuggestion)+" and suggests you to "+getPrintableFromAction(bestHumanActionSuggestion));
				//enableSend(true);
				addToGUIMessage("Would you like to accept the suggestion? (Y or N [A, B, C, D, E])");
				CommResponse response = getHumanMessage(bestHumanActionSuggestion);
				if(response.commType == CommType.NONE)
					outOfTimeMessage();
				actions = new HumanRobotActionPair(response.humanAction, bestRobotActionSuggestion);
				
			} else { //robot just updates
				numRobotUpdates++;
				//enableSend(false);
				updateGUIMessage("Your teammate will "+getPrintableFromAction(bestRobotActionUpdate));
				//enableSend(true);
				addToGUIMessage("Which fire you would like to extinguish (A, B, C, D, or E)?");
				CommResponse response = getHumanMessage(null);
				if(response.commType == CommType.NONE)
					outOfTimeMessage();
				actions = new HumanRobotActionPair(response.humanAction, bestRobotActionUpdate);			
			}
			updateGUIMessage("Summary:\nYou chose to "+getPrintableFromAction(actions.getHumanAction())+"\nYour teammate chose to "+getPrintableFromAction(actions.getRobotAction())+"\n");
			return actions;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Human's turn to initiate communication
	 * Can either enter "BOTH" to suggest a joint action or just update the robot
	 * If the human's suggestion is not much worse than the optimal, the robot will accept, otherwise it will reject
	 * If the human just updates, the robot will calculate the best action given the human's action
	 */
	public HumanRobotActionPair humanComm(State state, Action pastRobotAction) {
		try{
			updateGUIMessage("Which fire you would like to extinguish (A, B, C, D, E)? If you want to make a suggestion, add a space and one more letter the action you suggest for the robot: ");
			//enableSend(true);
			CommResponse response = getHumanMessage(null);			
			Action humanAction = response.humanAction;
			Action robotAction = response.robotAction;
			if(response.commType == CommType.UPDATE)
				updateGUIMessage("You would like to "+getPrintableFromAction(humanAction));
			else if(response.commType == CommType.SUGGEST)
				updateGUIMessage("You would like to "+getPrintableFromAction(humanAction)+" and suggest your teammate to "+getPrintableFromAction(robotAction));
			if(response.commType != CommType.NONE){
				if(response.commType == CommType.SUGGEST){
					double humanSuggestedQValue = getJointQValue(state, new HumanRobotActionPair(humanAction, robotAction));
					Action optimalRobotAction = null;
					if(pastRobotAction != null)
						optimalRobotAction = pastRobotAction;
					else
						optimalRobotAction = getGreedyRobotAction(state, humanAction);
					double robotSuggestedQValue = getJointQValue(state, new HumanRobotActionPair(humanAction, optimalRobotAction));
					//enableSend(false);
					addToGUIMessage("Waiting for teammate...");
					simulateWaitTime(state);
					//robot rejects human suggestion and chooses own action assuming human will do their suggested action
					if((robotSuggestedQValue - humanSuggestedQValue) > Constants.THRESHOLD_REJECT){ 
						numRobotRejects++;
						robotAction = optimalRobotAction;
						updateGUIMessage("Your teammate has a different preference and chooses to "+getPrintableFromAction(robotAction));
					} else {
						numRobotAccepts++;
						updateGUIMessage("Your teammate accepts to "+getPrintableFromAction(robotAction));
					}
				} else if(response.commType == CommType.UPDATE){
					numHumanUpdates++;				
					//enableSend(false);				
					addToGUIMessage("Waiting for teammate...");
					simulateWaitTime(state);
					robotAction = getGreedyRobotAction(state, humanAction);
					updateGUIMessage("Your teammate chose to "+getPrintableFromAction(robotAction));
				}
			} else {
				outOfTimeMessage();
			}
			Thread.sleep(3000);
			updateGUIMessage("Summary:\nYou chose to "+getPrintableFromAction(humanAction)+"\nYour teammate chose to "+getPrintableFromAction(robotAction)+"\n");
			return new HumanRobotActionPair(humanAction, robotAction);
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public CommResponse getHumanMessage(Action suggestedHumanAction){
		startTimer();
		CommResponse response = null;
		try {
			if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN)
				response = myServer.getHumanMessage(suggestedHumanAction);
			else if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN){
				Main.gameView.focusTextField();
				while(Main.gameView.humanMessage == null){
					System.out.print("");
				}
				String text = Main.gameView.humanMessage;
				Main.gameView.humanMessage = null;
				System.out.println("text received "+text);
				response = parseHumanInput(text.trim(), suggestedHumanAction);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		stopTimer();
		return response;
	}
	
	public CommResponse parseHumanInput(String text, Action suggestedHumanAction){
		CommType commType = CommType.NONE;
		Action humanAction = Action.WAIT;
		Action robotAction = Action.WAIT;
		String [] strs = text.split(" ");
		for(String str : strs)
			System.out.println("split "+str);
		if(strs[0].equalsIgnoreCase("Y")){
			commType = CommType.ACCEPT;
			humanAction = suggestedHumanAction;
		} else if(strs[0].equalsIgnoreCase("N")){
			commType = CommType.REJECT;
			humanAction = convertToAction(strs[1].toUpperCase().charAt(0));
		} else {
			commType = CommType.UPDATE;
			humanAction = convertToAction(strs[0].toUpperCase().charAt(0));
			if(strs.length > 1){
				commType = CommType.SUGGEST;
				robotAction = convertToAction(strs[1].toUpperCase().charAt(0));
			}
		}
		return new CommResponse(commType, humanAction, robotAction);
	}
	
	public Action convertToAction(char c){
		if(c >= 'A' && c <= 'E'){
			String actionStr = "PUT_OUT"+(c - 'A');
			return Action.valueOf(actionStr);
		}
		return Action.WAIT;
	}
	
	public void outOfTimeMessage(){
		updateGUIMessage("\nSorry you ran out of time!\n");
	}
	
	public void updateGUIMessage(String str){
		Main.gameView.setTeammateText(str);
	}
	
	public void addToGUIMessage(String str){
		Main.gameView.setTeammateText(Main.gameView.getTeammateText()+"\n"+str);
	}
	
	public void sendMessageToRobot(String str, int client){
		try{
			if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN){
				myServer.sendMessage(str, client);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds wait time to simulate a human playing
	 */
	public void simulateWaitTime(State state) {
//		int stateScore = 0;
//		for(int i=0; i<state.stateOfFires.length; i++){
//			int num = state.stateOfFires[i];
//			if(num == Constants.BURNOUT)
//				stateScore += 0;
//			else
//				stateScore += num;
//		}
//		System.out.println("score "+stateScore);
//		try{
//			if(stateScore < 10){
//				int shortRandomTime = 6; //Main.rand.nextInt(3)+5;
//				System.out.println(shortRandomTime*1000);
//				Thread.sleep(shortRandomTime*1000);
//			} else {
//				int longRandomTime = 8; //Main.rand.nextInt(5)+8;
//				System.out.println(longRandomTime*1000);
//				Thread.sleep(longRandomTime*1000);
//			}
//			
//		} catch(Exception e){
//			e.printStackTrace();
//		}
	}
	
	/**
	 * Computes a policy (indicating what action should be taken for each state) given the q value table
	 */
	public Policy computePolicy(){
		//HashMap<Integer, HumanRobotActionPair> policy = new HashMap<Integer, HumanRobotActionPair>();
		HumanRobotActionPair[] policy = new HumanRobotActionPair[mdp.states.size()];
		for(State state : mdp.states()){
			HumanRobotActionPair actions = getGreedyJointAction(state).getFirst();
			policy[state.getId()] = actions;
		}
		return new Policy(policy);
	}
	
	/**
	 * Computes the best robot action to perform 
	 * If a human action is provided, the best action will be computed given that human action
	 */
	public Action getGreedyRobotAction(State state, Action humanAction) {
		double maxValue = Integer.MIN_VALUE;
		List<Action> possibleRobotActions = new ArrayList<Action>();
		for(Action robotAction : mdp.robotAgent.actions(state)){
			double value = Integer.MIN_VALUE;
			if(humanAction == null)
				value = getRobotQValue(state, robotAction);
			else
				value = getJointQValue(state, new HumanRobotActionPair(humanAction, robotAction));
			if(value > maxValue){
				maxValue = value;
				possibleRobotActions.clear();
			}
			if(Math.abs(value - maxValue) < 0.001){
            	possibleRobotActions.add(robotAction); //basically equal
            }
		}
		return possibleRobotActions.get(Tools.rand.nextInt(possibleRobotActions.size()));
	}
	
	/**
	 * Computes the best joint action (the one with the highest q value for this particular state)
	 */
	public Pair<HumanRobotActionPair, Double> getGreedyJointAction(State state) {
		double maxValue = Integer.MIN_VALUE;
		List<HumanRobotActionPair> possibleActions = new ArrayList<HumanRobotActionPair>();
		for(Action humanAction : mdp.humanAgent.actions(state)){
			for(Action robotAction : mdp.robotAgent.actions(state)){
				HumanRobotActionPair actionPair = new HumanRobotActionPair(humanAction, robotAction);
				double value = getJointQValue(state, actionPair);
				if(value > maxValue){
					maxValue = value;
					possibleActions.clear();
				}
				if(Math.abs(value - maxValue) < 0.001){ //basically equal
	            	possibleActions.add(actionPair);
	            }
			}
		}
		return new Pair<HumanRobotActionPair, Double>(possibleActions.get(Tools.rand.nextInt(possibleActions.size())), maxValue);
	}

	/**
	 * Computes the max robot value for this state
	 */
	public double maxRobotQ(State state) {
		double maxValue = Integer.MIN_VALUE;
		for(Action robotAction : mdp.robotAgent.actions(state)){
			double value = getRobotQValue(state, robotAction);
			if(value > maxValue){
				maxValue = value;
			}
		}
		return maxValue;
    }
	
	/**
	 * Computes the max joint value for this state
	 */
	public double maxJointQ(State state) {
		double maxValue = Integer.MIN_VALUE;
		for(Action humanAction : mdp.humanAgent.actions(state)){
			for(Action robotAction : mdp.robotAgent.actions(state)){
				double value = getJointQValue(state, new HumanRobotActionPair(humanAction, robotAction));
				if(value > maxValue){
					maxValue = value;
				}
			}
		}
		return maxValue;
    }
	
	public double getRobotQValue(State state, Action robotAction){
		return robotQValues[state.getId()][robotAction.ordinal()];
	}
	
	public double getJointQValue(State state, HumanRobotActionPair agentActions){
		return jointQValues[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
	}
	
	public void saveDataToFile(double reward, int iterations, long time){
		try{
			BufferedWriter rewardHumanWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardHumanName), true));
			BufferedWriter iterHumanWriter = new BufferedWriter(new FileWriter(new File(Constants.iterHumanName), true));
			BufferedWriter timeWriter = new BufferedWriter(new FileWriter(new File(Constants.timeName), true));
			BufferedWriter robotUpdatesWriter = new BufferedWriter(new FileWriter(new File(Constants.robotUpdatesName), true));
			BufferedWriter robotSuggWriter = new BufferedWriter(new FileWriter(new File(Constants.robotSuggName), true));
			BufferedWriter humanUpdatesWriter = new BufferedWriter(new FileWriter(new File(Constants.humanUpdatesName), true));
			BufferedWriter humanSuggWriter = new BufferedWriter(new FileWriter(new File(Constants.humanSuggName), true));
			BufferedWriter robotAccWriter = new BufferedWriter(new FileWriter(new File(Constants.robotAccName), true));
			BufferedWriter robotRejWriter = new BufferedWriter(new FileWriter(new File(Constants.robotRejName), true));
			BufferedWriter humanAccWriter = new BufferedWriter(new FileWriter(new File(Constants.humanAccName), true));
			BufferedWriter humanRejWriter = new BufferedWriter(new FileWriter(new File(Constants.humanRejName), true));
	
	    	robotUpdatesWriter.write(numRobotUpdates+", ");
			robotSuggWriter.write(numRobotSuggestions+", ");
			humanUpdatesWriter.write(numHumanUpdates+", ");
			humanSuggWriter.write(numHumanSuggestions+", ");
			
			robotAccWriter.write(numRobotAccepts+", ");
			robotRejWriter.write(numRobotRejects+", ");
			humanAccWriter.write(numHumanAccepts+", ");
			humanRejWriter.write(numHumanRejects+", ");
			
			rewardHumanWriter.write(reward+", ");
	        iterHumanWriter.write(iterations+", ");
	    	timeWriter.write(time+", ");
	    	
	    	rewardHumanWriter.close();
	        iterHumanWriter.close();
	        timeWriter.close();
	        robotUpdatesWriter.close();
			robotSuggWriter.close();
			humanUpdatesWriter.close();
			humanSuggWriter.close();
			robotAccWriter.close();
			robotRejWriter.close();
			humanAccWriter.close();
			humanRejWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void saveEpisodeToFile(State state, Action humanAction, Action robotAction, State nextState, double reward){
		/*try{
			if(withHuman && Main.saveToFile){
				BufferedWriter episodeWriter = new BufferedWriter(new FileWriter(new File(episodeName), true));
				episodeWriter.write(state.toStringFile()+", "+humanAction+", "+robotAction+", "
						+nextState.toStringFile()+", "+reward+"\n");
				episodeWriter.close();
	        }
		} catch(Exception e){
			e.printStackTrace();
		}*/
	}
	
	public void resetCommunicationCounts(){
		numRobotSuggestions = 0;
		numRobotUpdates = 0;
		numHumanSuggestions = 0;
		numHumanUpdates = 0;
		
		numRobotAccepts = 0;
		numRobotRejects = 0;
		numHumanAccepts = 0;
		numHumanRejects = 0;
	}
	
	public void startTimer(){
		timeLeft = Constants.MAX_TIME;
		Main.gameView.setTime(timeLeft);
	    timer.start();
	}
	
	public void stopTimer(){
		Main.gameView.setTime(timeLeft);
		timer.stop();
		timeLeft = Constants.MAX_TIME;
	}
	
	public ActionListener timerListener() {
		return new ActionListener() {
		  public void actionPerformed(ActionEvent evt) {
			  timeLeft--;
			  Main.gameView.setTime(timeLeft);
		      if(timeLeft == 0){
		    	  timer.stop();
		      }
		  }
		};
	}
}
