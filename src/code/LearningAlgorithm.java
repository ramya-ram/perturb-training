package code;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import matlabcontrol.MatlabInvocationException;

/**
 * Parent class for QLearner and PolicyReuseLearner
 */
public class LearningAlgorithm {
	protected MyWorld myWorld = null;
	protected MDP mdp;
	
	public QValuesSet currQValues; //the current Q-values being used
	public List<QValuesSet> qValuesList; //holds a list of Q-values, in procedural there's only one

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
	
	public ExperimentCondition condition;
	
	/**
	 * Run one episode of the task (initial state to goal state) with e-greedy Q-learning
	 */
	public Tuple<Double, Integer, Long> run(int maxSteps, State initialStateHuman, int episodeNum, List<QValuesSet> trainedLearners){
		return run(false, maxSteps, initialStateHuman, episodeNum, trainedLearners);
	}

	/**
	 * Runs one episode of the task (initial state to goal state) 
	 * Can either be e-greedy Q-learning (explore epsilon% of the time) or full greedy Q-learning (no exploration, 100% exploitation)
	 */
	public Tuple<Double, Integer, Long> run(boolean fullyGreedy, int maxSteps, State initialStateHuman, int episodeNum, List<QValuesSet> trainedLearners){
        double episodeReward = 0;
        int iterations = 0;
        long startTime = System.currentTimeMillis();
        
        //Resets anything in the task before the next episode, if needed
        myWorld.reset();
        
		State state = null;
		if(initialStateHuman != null && Main.CURRENT_EXECUTION != Main.SIMULATION) //use a specific initial state if given (useful for consistency in human subject experiments)
			state = initialStateHuman.clone();
		else //otherwise, randomly choose an initial state
			state = myWorld.initialState().clone();
		if(Main.arduino != null && Main.currWithSimulatedHuman)
			Main.arduino.sendString(state.getArduinoString());
        try{
	        while (!myWorld.isGoalState(state) && iterations < maxSteps) { //loop until the the goal state is reached or until the max number of steps is reached
	        	HumanRobotActionPair agentActions = null;
				if(withHuman && Main.CURRENT_EXECUTION != Main.SIMULATION) {
					agentActions = getAgentActionsCommWithHuman(state); //communicates with human to choose action
				} else if(fullyGreedy){
					agentActions = getAgentActionsFullyGreedySimulation(state); //uses greedy approach (no exploration, 100% exploitation)
				} else {
					agentActions = getAgentActionsSimulation(state); //uses e-greedy approach (with probability epsilon, choose a random action, otherwise exploit/choose optimal action) 
				}
				
				//get next state and reward and update Q-values
	            State nextState = myWorld.getNextState(state, agentActions);
	            double reward = myWorld.reward(state, agentActions, nextState);
	            episodeReward += reward;
	            saveEpisodeToFile(state, agentActions.getHumanAction(), agentActions.getRobotAction(), nextState, reward, episodeNum, trainedLearners);     
	            updateQValues(state, agentActions, nextState, reward);
	            
	            state = nextState.clone();
	            if(Main.arduino != null && Main.currWithSimulatedHuman)
	            	Main.arduino.sendString(state.getArduinoString());
	            iterations++;
	            
				if(withHuman && Main.gameView != null){
					Main.gameView.setNextEnable(true);
					Main.gameView.waitForNextClick();
					if(myWorld.isGoalState(state)){
						Main.gameView.initTitleGUI("congrats");
					}
					else if(iterations >= maxSteps)
						Main.gameView.initTitleGUI("roundUp");
				}
	        }
        } catch(Exception e){
        	e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        return new Tuple<Double,Integer,Long>(episodeReward, iterations, (endTime - startTime));
	}
	
	/**
	 * Compute a policy (indicating what action should be taken for each state) using the Q-value function
	 */
	public Policy computePolicy(){
		HumanRobotActionPair[] policy = new HumanRobotActionPair[mdp.states.size()];
		for(State state : mdp.states()){
			HumanRobotActionPair actions = getGreedyJointAction(state).getFirst();
			policy[state.getId()] = actions;
		}
		return new Policy(policy);
	}
	
	/**
	 * Update the joint Q-values and the robot Q-values based on an interaction
	 */
	public void updateQValues(State state, HumanRobotActionPair agentActions, State nextState, double reward){
		//if there's a library of Q-value functions, update all of them
		if(qValuesList != null){
			QValuesSet temp = currQValues;
			for(QValuesSet set : qValuesList){
				currQValues = set;
				updateOneQValuesSet(state, agentActions, nextState, reward);
			}
			currQValues = temp;
		} else { //otherwise, just update the current one
			updateOneQValuesSet(state, agentActions, nextState, reward);
		}
	}
	
	/**
	 * Update the current Q-values set (both the robot and joint Q-values) for this particular state, joint action, and next state
	 */
	public void updateOneQValuesSet(State state, HumanRobotActionPair agentActions, State nextState, double reward){
		int humanAction = agentActions.getHumanAction().ordinal();
		int robotAction = agentActions.getRobotAction().ordinal();
		int stateId = state.getId();
		
		double robotQ = getRobotQValue(state, agentActions.getRobotAction());
		double robotMaxQ = maxRobotQ(nextState);	 
        double robotValue = (1 - Constants.ALPHA) * robotQ + Constants.ALPHA * (reward + Constants.GAMMA * robotMaxQ);
        currQValues.robotQValues[stateId][robotAction] = robotValue;
        
        double jointQ = getJointQValue(state, agentActions);
		double jointMaxQ = maxJointQ(nextState);
		double jointValue = (1 - Constants.ALPHA) * jointQ + Constants.ALPHA * (reward + Constants.GAMMA * jointMaxQ);
		currQValues.jointQValues[stateId][humanAction][robotAction] = jointValue;
	}
	
	/**
	 * Simulates interactions with the human independently
	 * Either chooses the best joint action or randomly chooses a joint action (with probability epsilon)
	 */
	public HumanRobotActionPair getAgentActionsSimulation(State state){
		HumanRobotActionPair proposedJointAction = null;
		if(Constants.rand.nextDouble() < Constants.EPSILON){
			Action[] possibleRobotActions = mdp.robotAgent.actions(state);
			Action[] possibleHumanActions = mdp.humanAgent.actions(state); //from robot state because that's what robot sees
	        Action robotAction = possibleRobotActions[Constants.rand.nextInt(possibleRobotActions.length)];
	        Action humanAction = possibleHumanActions[Constants.rand.nextInt(possibleHumanActions.length)];
	        proposedJointAction = new HumanRobotActionPair(humanAction, robotAction);
		} else { // otherwise, choose the best action/the one with the highest q value
			Pair<HumanRobotActionPair, Double> proposed = getGreedyJointAction(state);
			proposedJointAction = proposed.getFirst();
		}
		return proposedJointAction;
	}
	
	/**
	 * Gets the best greedy joint action (no probability of random actions)
	 */
	public HumanRobotActionPair getAgentActionsFullyGreedySimulation(State state){
		Pair<HumanRobotActionPair, Double> proposed = getGreedyJointAction(state);
		return proposed.getFirst();
	}
	
	/**
	 * Prints out Q-values of a particular state, for debugging purposes
	 */
	public void numOfNonZeroQValues(State state, String fileName, boolean writeToFile){
		if(writeToFile){
			try{
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(Constants.simulationDir+fileName+".txt")));
				if(qValuesList != null){
					for(QValuesSet set : qValuesList){
						writeQValuesToFile(writer, state, set);
						writer.write("\n\n");
					}
				} else {
					writeQValuesToFile(writer, state, currQValues);
				}
				writer.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Write Q-values to a file
	 */
	public void writeQValuesToFile(BufferedWriter writer, State state, QValuesSet qValuesSet){
		try{
			int stateId = state.getId();
			for(int j=0; j<qValuesSet.jointQValues[stateId].length; j++){
				for(int k=0; k<qValuesSet.jointQValues[stateId][j].length; k++){
					if(qValuesSet.jointQValues[stateId][j][k] < 0 || qValuesSet.jointQValues[stateId][j][k] > 0){
						writer.write("jointQValues["+state.toString()+" "+stateId+"]["+j+"]["+k+"] = "+qValuesSet.jointQValues[stateId][j][k]+"\n");
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Get index of the action
	 */
	public int getActionIndex(Action action){
		if(action != Action.WAIT)
			return action.ordinal();
		return -1;
	}
	
	/**
	 * The human and robot communicate to choose a joint action for this state
	 * The final joint action decided upon is then returned
	 */
	public HumanRobotActionPair getAgentActionsCommWithHuman(State state){
		try{
			if(Main.gameView != null){
				MyWorld.updateState(state);
				Main.gameView.setAnnouncements("");
				Main.gameView.setTeammateText("");
			}
			HumanRobotActionPair actions = null;
			if(currCommunicator == Constants.HUMAN){
				actions = humanComm(state); //the human initiates (decides whether to suggest or update)
				currCommunicator = Constants.ROBOT; //the human and robot alternate in initiating communication
			} else if(currCommunicator == Constants.ROBOT){
				actions = robotComm(state); //the robot initiates
				currCommunicator = Constants.HUMAN;
			}
			timer.stop();
			if(Main.gameView != null){
				Main.gameView.setTime(-1);
				if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN_TEST && actions.getRobotAction() != Action.WAIT){
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
	
	/**
	 * Robot's turn to initiate communication
	 * If the computed joint action is better than the average value, the robot will suggest a joint action, which the human can accept or reject
	 * Otherwise, the robot will just update the human on its action and the human can choose accordingly
	 */
	public HumanRobotActionPair robotComm(State state) {
		try{
			HumanRobotActionPair actions;
			double maxJointValue = Integer.MIN_VALUE;
			Action bestHumanActionSuggestion = null;
			Action bestRobotActionSuggestion = null;
			Action bestRobotActionUpdate = null;
			double cumulativeValue = 0;
			Action[] humanActions = mdp.humanAgent.actions(state); //all possible human actions for this state
			
			//calculate the best joint action
			Pair<HumanRobotActionPair, Double> pair = getGreedyJointAction(state);
			HumanRobotActionPair agentActions = pair.getFirst();
			maxJointValue = pair.getSecond();
			bestHumanActionSuggestion = agentActions.getHumanAction();
			bestRobotActionSuggestion = agentActions.getRobotAction();
			
			//calculate the best robot action without knowing the human's action
			bestRobotActionUpdate = getGreedyRobotAction(state, null);
			for(Action humanAction : humanActions){
				double value = getJointQValue(state, new HumanRobotActionPair(humanAction, bestRobotActionUpdate));
				cumulativeValue += value;
			}
			//calculate the average value of the robot taking the optimal action and the human taking any possible action	
			double averageValue = cumulativeValue/humanActions.length;
			
			updateGUIMessage("Waiting for teammate...\n");
			myWorld.simulateWaitTime(state);
			
			//if the value of the best joint action is much better (determined by THRESHOLD_SUGG) than the average over all human actions where robot acts optimally, robot makes a suggestion
			if((maxJointValue - averageValue) > Constants.THRESHOLD_SUGG && bestHumanActionSuggestion != null){
				numRobotSuggestions++;
				//this message is sent to the embodied robot to trigger robot speech and movement for that action
				sendRobotMessage("{SUGGESTION R"+getActionIndex(bestRobotActionSuggestion)+", H"+getActionIndex(bestHumanActionSuggestion)+"}");
				//this updates the GUI displayed to the person
				updateGUIMessage("Your teammate will "+myWorld.getPrintableFromAction(bestRobotActionSuggestion)+" and suggests you to "+myWorld.getPrintableFromAction(bestHumanActionSuggestion));
				addToGUIMessage("Would you like to accept the suggestion? (Y or N _)");
				CommResponse response = getHumanMessage(bestHumanActionSuggestion, state); //get human response of accept or reject
				if(response.commType == CommType.ACCEPT)
					numHumanAccepts++;
				else if(response.commType == CommType.REJECT)
					numHumanRejects++;
				actions = new HumanRobotActionPair(response.humanAction, bestRobotActionSuggestion);
				
			} else { //if the value of the best joint action is not much better than the average, maybe not useful to suggest so the robot just updates the person on its own action
				numRobotUpdates++;
				sendRobotMessage("{UPDATE R"+getActionIndex(bestRobotActionUpdate)+"}");
				updateGUIMessage("Your teammate will "+myWorld.getPrintableFromAction(bestRobotActionUpdate));
				addToGUIMessage("Which fire you would like to extinguish (_)?");
				CommResponse response = getHumanMessage(null, state);
				actions = new HumanRobotActionPair(response.humanAction, bestRobotActionUpdate);			
			}
			sendRobotMessage("{** R"+getActionIndex(actions.getRobotAction())+"}");
			updateGUIMessage("Summary:\nYou will "+myWorld.getPrintableFromAction(actions.getHumanAction())+"\nYour teammate will "+myWorld.getPrintableFromAction(actions.getRobotAction())+"\n");
			return actions;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Sends a message to the embodied robot, the PR2, to trigger speech and movement for that action
	 */
	public void sendRobotMessage(String str) {
		if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN_TEST){
			try{
				Main.myServer.sendMessage(str, Constants.ROBOT);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Human's turn to initiate communication
	 * Can either enter "BOTH" to suggest a joint action or just update the robot
	 * If the human's suggestion is not much worse than the optimal, the robot will accept, otherwise it will reject
	 * If the human just updates, the robot will calculate the best action given the human's action
	 */
	public HumanRobotActionPair humanComm(State state) {
		try{
			updateGUIMessage("Which fire you would like to extinguish (_)? If you want to make a suggestion, (_ _): ");
			//gets the human's message
			CommResponse response = getHumanMessage(null, state);			
			Action humanAction = response.humanAction;
			Action robotAction = response.robotAction;
			//confirm on the GUI what the person chose
			if(response.commType == CommType.UPDATE)
				updateGUIMessage("You chose to "+myWorld.getPrintableFromAction(humanAction));
			else if(response.commType == CommType.SUGGEST)
				updateGUIMessage("You chose to "+myWorld.getPrintableFromAction(humanAction)+" and suggest your teammate to "+myWorld.getPrintableFromAction(robotAction));
			if(response.commType != CommType.NONE){
				if(response.commType == CommType.SUGGEST){ //if the human made a suggestion
					numHumanSuggestions++;
					
					//calculate the value of the human's suggested joint action
					double humanSuggestedQValue = getJointQValue(state, new HumanRobotActionPair(humanAction, robotAction));
					//calculate the value if the robot rejects the human's suggestion and chooses the optimal action (human still takes the same action)
					Action optimalRobotAction = getGreedyRobotAction(state, humanAction);
					double robotSuggestedQValue = getJointQValue(state, new HumanRobotActionPair(humanAction, optimalRobotAction));
					
					addToGUIMessage("Waiting for teammate...");
					myWorld.simulateWaitTime(state);
					//if the robot gains a lot from rejecting the human's suggestion and choosing the optimal action (determined by THRESHOLD_REJECT), then the robot rejects
					if((robotSuggestedQValue - humanSuggestedQValue) > Constants.THRESHOLD_ACCEPT){ 
						numRobotRejects++;
						robotAction = optimalRobotAction;
						sendRobotMessage("{REJECT R"+getActionIndex(robotAction)+"}");
						updateGUIMessage("Your teammate has a different preference and will "+myWorld.getPrintableFromAction(robotAction)+"\n");
					} else { //if the robot doesn't gain much by rejecting, the robot accepts the human's suggestion
						numRobotAccepts++;
						sendRobotMessage("{ACCEPT R"+getActionIndex(robotAction)+"}");
						updateGUIMessage("Your teammate accepts to "+myWorld.getPrintableFromAction(robotAction)+"\n");
					}
				} else if(response.commType == CommType.UPDATE){ //if the human made an update (did not suggest an action for the robot)
					numHumanUpdates++;				
					addToGUIMessage("Waiting for teammate...");
					myWorld.simulateWaitTime(state);
					robotAction = getGreedyRobotAction(state, humanAction); //the robot chooses the action with maximum value given that the human will take the specified action
					sendRobotMessage("{UPDATE R"+getActionIndex(robotAction)+"}");
					updateGUIMessage("Your teammate will "+myWorld.getPrintableFromAction(robotAction)+"\n");
				}
			}
			sendRobotMessage("{* R"+getActionIndex(robotAction)+"}");
			addToGUIMessage("Summary:\nYou will "+myWorld.getPrintableFromAction(humanAction)+"\nYour teammate will "+myWorld.getPrintableFromAction(robotAction)+"\n");
			return new HumanRobotActionPair(humanAction, robotAction);
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Gets the human's message
	 */
	public CommResponse getHumanMessage(Action suggestedHumanAction, State currState){
		startTimer();
		CommResponse response = null;
		try {
			//get message from PR2 if running hardware robot experiments
			if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN_TEST){
				response = Main.myServer.getHumanMessage(suggestedHumanAction);
			} else if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN_TRAIN_TEST || Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN_TRAIN){
				//get message from GUI if running simulated robot experiments
				response = getGUIMessage(suggestedHumanAction, currState);
				while(LearningAlgorithm.timeLeft > 0 && ((response.humanAction == Action.WAIT))){
					addToGUIMessage("Invalid input, please specify again what you would like to do!");
					response = getGUIMessage(suggestedHumanAction, currState);
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		stopTimer();
		return response;
	}
	
	/**
	 * Get GUI message from human
	 */
	public CommResponse getGUIMessage(Action suggestedHumanAction, State currState) {
		CommResponse response = null;
		Main.gameView.setTextFieldEnable(true);
		Main.gameView.focusTextField();
		while(Main.gameView.humanMessage == null){
			System.out.print("");
			if(LearningAlgorithm.timeLeft == 0){ //if time runs out, human does a wait action
				System.out.println("time over");
				outOfTimeMessage();
				Main.gameView.setTextFieldEnable(false);
				return new CommResponse(CommType.NONE, Action.WAIT, Action.WAIT);	
			}
		}
		String text = Main.gameView.humanMessage;
		Main.gameView.humanMessage = null;
		System.out.println("text received "+text);
		response = parseHumanInput(text.trim(), suggestedHumanAction, currState);
		Main.gameView.setTextFieldEnable(false);
		return response;
	}
	
	/**
	 * Parse text the human entered while working with the simulated robot
	 * Extract the communication type and the human and robot action from this text
	 */
	public CommResponse parseHumanInput(String text, Action suggestedHumanAction, State currState){
		CommType commType = CommType.NONE;
		Action humanAction = Action.WAIT;
		Action robotAction = Action.WAIT;
		text = text.trim();
		String [] strs = text.split(" ");
		for(String str : strs)
			System.out.println("split "+str);
		if(strs[0].equalsIgnoreCase("Y")){ //If person types 'Y' or 'y', this is an accept communication action
			commType = CommType.ACCEPT;
			if(suggestedHumanAction != null)
				humanAction = suggestedHumanAction;
		} else if(strs[0].equalsIgnoreCase("N")){ //If person types 'N' or 'n', this is a reject communication action
			commType = CommType.REJECT;
			if(strs.length>=2)
				if(strs[1].length() > 0)
					humanAction = convertToAction(strs[1].toUpperCase(), mdp.humanAgent.actionsAsList(currState));
		} else {
			commType = CommType.UPDATE;
			if(strs.length>=1) //if person types one letter, this is an update communication action
				if(strs[0].length() > 0)
					humanAction = convertToAction(strs[0].toUpperCase(), mdp.humanAgent.actionsAsList(currState));
			if(strs.length > 1){ //if person types multiple letters, this is a suggestion communication action
				commType = CommType.SUGGEST;
				if(strs[1].length() > 0)
					robotAction = convertToAction(strs[1].toUpperCase(), mdp.robotAgent.actionsAsList(currState));
			}
		}
		return new CommResponse(commType, humanAction, robotAction);
	}
	
	/**
	 * Convert entered string to the appropriate action and check if the action is a possible/allowed action
	 */
	public Action convertToAction(String str, List<Action> possibleActions){
		str = str.trim();
		System.out.println("converting "+str);
		if(str.equalsIgnoreCase("A") || str.equalsIgnoreCase("B") || str.equalsIgnoreCase("C") || str.equalsIgnoreCase("D") || str.equalsIgnoreCase("E")){
			int index = getInt(str);
			String actionStr = "PUT_OUT"+index;
			Action action = Action.WAIT;
			if(index >= 0)
				action = Action.valueOf(actionStr);
			if(possibleActions.contains(action))
				return action;
			return Action.WAIT;
		}
		return Action.WAIT;
	}
	
	/**
	 * Converts the string to the corresponding integer value
	 */
	public int getInt(String str){
		if(str.equalsIgnoreCase("A"))
			return 0;
		else if(str.equalsIgnoreCase("B"))
			return 1;
		else if(str.equalsIgnoreCase("C"))
			return 2;
		else if(str.equalsIgnoreCase("D"))
			return 3;
		else if(str.equalsIgnoreCase("E"))
			return 4;
		return -1;
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
	
	/**
	 * Computes the best robot action to perform 
	 * If a human action is provided, the best action will be computed given that human action
	 */
	public Action getGreedyRobotAction(State state, Action humanAction) {
		double maxValue = Integer.MIN_VALUE;
		List<Action> possibleRobotActions = new ArrayList<Action>();
		for(Action robotAction : mdp.robotAgent.actions(state)){
			double value = Integer.MIN_VALUE;
			//if no human action if given, we use the robot value function Q(s,a_r) and find the robot action with maximum value regardless of what human does
			if(humanAction == null)
				value = getRobotQValue(state, robotAction);
			else //if a human action is given, we use the joint value function Q(s, a_h, a_r) and find the robot action with maximum value given the human action
				value = getJointQValue(state, new HumanRobotActionPair(humanAction, robotAction));
			if(value > maxValue){
				maxValue = value;
				possibleRobotActions.clear();
			}
			if(Math.abs(value - maxValue) < 0.001){
            	possibleRobotActions.add(robotAction); //basically equal
            }
		}
		return possibleRobotActions.get(Constants.rand.nextInt(possibleRobotActions.size()));
	}
	
	/**
	 * Computes the best joint action (the one with the highest Q-value for this particular state)
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
		return new Pair<HumanRobotActionPair, Double>(possibleActions.get(Constants.rand.nextInt(possibleActions.size())), maxValue);
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
		return currQValues.robotQValues[state.getId()][robotAction.ordinal()];
	}
	
	public double getJointQValue(State state, HumanRobotActionPair agentActions){
		return currQValues.jointQValues[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
	}
	
	/**
	 * Saves data from human subject experiments into a file
	 */
	public void saveDataToFile(double reward, int iterations, long time){
		try{
			BufferedWriter rewardHumanWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"RewardHuman.csv"), true));
			BufferedWriter iterHumanWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"IterHuman.csv"), true));
			BufferedWriter timeWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"Time.csv"), true));
			BufferedWriter robotUpdatesWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"robotUpdates.csv"), true));
			BufferedWriter robotSuggWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"robotSuggestions.csv"), true));
			BufferedWriter humanUpdatesWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"humanUpdates.csv"), true));
			BufferedWriter humanSuggWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"humanSuggestions.csv"), true));
			BufferedWriter robotAccWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"robotAccepts.csv"), true));
			BufferedWriter robotRejWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"robotRejects.csv"), true));
			BufferedWriter humanAccWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"humanAccepts.csv"), true));
			BufferedWriter humanRejWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"humanRejects.csv"), true));
	
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
	
	public static void writeToFile(String fileName, String dataToWrite){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName), true));
			writer.write(dataToWrite);
			writer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Saves the episode, iteration by iteration, for human subject experiments and for sampling data points
	 */
	public void saveEpisodeToFile(State state, Action humanAction, Action robotAction, State nextState, double reward, int episodeNum, List<QValuesSet> trainedLearners){
		try{
			if(Main.CURRENT_EXECUTION == Main.SIMULATION && condition == ExperimentCondition.PRQL_RBM){
				if(Main.currRBMDataNum == Constants.NUM_RBM_DATA_POINTS){
					if(myWorld.typeOfWorld == Constants.TESTING){
						int closestMDPNum = (int)((double[]) Main.proxy.returningFeval("runRBM", 1, Main.RBMTrainTaskData, Main.RBMTestTaskData[myWorld.sessionNum-1], Constants.NUM_HIDDEN_UNITS)[0])[0] - 1;
						System.out.println("task "+(myWorld.sessionNum-1)+" closestMDP "+closestMDPNum);
						currQValues = trainedLearners.get(closestMDPNum).clone();
						Main.closestTrainingTask[condition.ordinal()][myWorld.sessionNum-1] = closestMDPNum;
						Main.currRBMDataNum++;
					} else {
						Main.currRBMDataNum = 0;
					}
				}
				
				if(Main.writeRBMDataToFile){
					File file = new File(myWorld.fileName);
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					writer.write(state.toStringRBM()+","+humanAction.ordinal()+","+robotAction.ordinal()+","+nextState.toStringRBM()+"\n");
					writer.close();
				}
				
				if(Main.currRBMDataNum < Constants.NUM_RBM_DATA_POINTS) {
					int[][][] RBMDataPoints = null;
					if(myWorld.typeOfWorld == Constants.TRAINING)
						RBMDataPoints = Main.RBMTrainTaskData;
					else if(myWorld.typeOfWorld == Constants.TESTING)
						RBMDataPoints = Main.RBMTestTaskData;
					
					int[] stateToArray = state.toArrayRBM();
					for(int i=0; i<stateToArray.length; i++)
						RBMDataPoints[myWorld.sessionNum-1][Main.currRBMDataNum][i] = stateToArray[i];
					RBMDataPoints[myWorld.sessionNum-1][Main.currRBMDataNum][stateToArray.length] = humanAction.ordinal();
					RBMDataPoints[myWorld.sessionNum-1][Main.currRBMDataNum][stateToArray.length+1] = robotAction.ordinal();
					int[] nextStateToArray = nextState.toArrayRBM();
					for(int i=0; i<nextStateToArray.length; i++)
						RBMDataPoints[myWorld.sessionNum-1][Main.currRBMDataNum][stateToArray.length+2+i] = nextStateToArray[i];
					
					Main.currRBMDataNum++;
				}
			}
			else if(withHuman && Main.saveToFile){
				BufferedWriter episodeWriter = new BufferedWriter(new FileWriter(new File(Constants.participantDir+"episode.txt"), true));
				episodeWriter.write(state.toString()+", "+humanAction+", "+robotAction+", "
						+nextState.toString()+", "+reward+"\n");
				episodeWriter.close();
	        }
		} catch(MatlabInvocationException e){
			Main.initMatlabProxy();
			saveEpisodeToFile(state, humanAction, robotAction, nextState, reward, episodeNum, trainedLearners);
		} catch(Exception e){
			e.printStackTrace();
		}
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
