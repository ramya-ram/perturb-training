package code;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.Timer;

/**
 * Parent class for QLearner and PolicyReuseLearner
 */
public class LearningAlgorithm {
	protected MyWorld myWorld = null;
	protected MDP mdp;
	public SocketConnect connect;	
	public double[][] robotQValues; 
	public double[][][] jointQValues;

	protected boolean communication;
	protected int currCommunicator = 1; //human = 0, robot = 1
	public Scanner scan = new Scanner(System.in);
	public static double THRESHOLD_SUGG = 0;
	public static double THRESHOLD_REJECT = 4;
	
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
	        	//if(myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM)
	        	//	System.out.println("state "+state.toStringFile());
				//if(withHuman && !reachedGoalState)
				//	agentActions = getAgentActionsCommWithHuman(state, null); //communicates with human to choose action until goal state is reached (and then it's simulated until maxSteps)
				//else{
					if(fullyGreedy)
						agentActions = getAgentActionsFullyGreedySimulation(state); //for policy reuse, fully greedy is used
					else
						agentActions = getAgentActionsSimulation(state); //uses e-greedy approach (with probability epsilon, choose a random action) 
				//}
				
	            State nextState = myWorld.getNextState(state, agentActions);
	            double reward = myWorld.reward(state, agentActions, nextState);
	            rewardPerEpisode+=reward;
	            saveEpisodeToFile(state, agentActions.getHumanAction(), agentActions.getRobotAction(), nextState, reward);     
	            updateQValues(state, agentActions, nextState, reward);
//	            if(myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM){
//	            	System.out.println(state.toStringFile()+": "+agentActions+" = "+reward);
//	            }
	            
	            state = nextState.clone();
	            count++;
	            
            	if(!reachedGoalState){
					if(MyWorld.isGoalState(state)){
						iterations = count;
						reachedGoalState = true;
					
					}
					
            	}
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
	 * Checks whether human input is valid
	 */
	public boolean isValid(String str, List<Action> possibleActions) {
		Action action = getActionFromStr(str, possibleActions); 
		return action != null || str.equals("BOTH");
	}
	
	/**
	 * Converts the input into an integer
	 */
	public static int convertToInt(String str){
		return str.charAt(0) - 'A';
	}
	
	/**
	 * Converts an index to the corresponding letter
	 */
	public static char convertToLetter(int index){
		return (char)(index + 'A');
	}
	
	/**
	 * Converts human input to an action
	 */
	public Action getActionFromStr(String entered, List<Action> possibleActions) {
		String enteredAction = "";
		if(entered.equals("NONE")){
			return Action.WAIT;
		}
		if(entered.equals("A") || entered.equals("B") || entered.equals("C") || entered.equals("D") || entered.equals("E")){
			enteredAction = "PUT_OUT"+convertToInt(entered);
		}
		if(enteredAction.length() == 0)
			return null;
		Action resultAction = Action.valueOf(Action.class, enteredAction);
		if(!possibleActions.contains(resultAction))
			return null;
		return resultAction;
	}
	
	/**
	 * Handles incorrect input
	 */
	public Action getCorrectedResponse(List<Action> possibleActions) {
		Action action = null;
		while(action == null){
			try {
				connect.sendMessage("Sorry that was an invalid response, please enter again: ");
				String entered = connect.getMessage().trim().toUpperCase();
				action = getActionFromStr(entered, possibleActions);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		return action;
	}
	
	public String getPrintableFromAction(Action action){
		if(action != Action.WAIT){
			int fireIndex = Integer.parseInt(action.name().substring(7, 8));
			return "extinguish "+convertToLetter(fireIndex);
		}
		return "wait.";
	}
	
	public boolean isValidResponseToSuggestion(String str){
		return str.equals("Y") || str.equals("N") || str.equals("NONE");
	}
	
	/**
	 * Prints to SocketTest to get human input
	 * The human and robot communicate to choose a joint action for this state
	 */
	public HumanRobotActionPair getAgentActionsCommWithHuman(State state, Action pastRobotAction){
		try{
			connect.sendMessage("-------------------------------------");
			connect.sendMessage("Fire Names:  A B C D E");
			connect.sendMessage(""+state);
			if(state.anyItemInState(MyWorld.BURNOUT))
				connect.sendMessage("-------------------------------------\nOh no! One or more of your buildings have burned out! All of the people have died there! "
						+ "\n-------------------------------------\n");
			HumanRobotActionPair actions = null;
			if(currCommunicator == 0){
				actions = humanComm(state, pastRobotAction);
				currCommunicator = 1;
			} else if(currCommunicator == 1){
				actions = robotComm(state, pastRobotAction);
				currCommunicator = 0;
			}
			timer.stop();
			Main.st.server.timeDisplay.setText("");
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
	public HumanRobotActionPair robotComm(State state, Action pastRobotAction) {
		try{
			HumanRobotActionPair actions;
			double maxJointValue = Integer.MIN_VALUE;
			Action bestHumanAction = null;
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
						bestHumanAction = humanAction;
					}
				}
			} else {
				System.out.println("not past robotAction "+pastRobotAction);
				Pair<HumanRobotActionPair, Double> pair = getGreedyJointAction(state);
				HumanRobotActionPair agentActions = pair.getFirst();
				maxJointValue = pair.getSecond();
				bestHumanAction = agentActions.getHumanAction();
				bestRobotActionSuggestion = agentActions.getRobotAction();
				
				bestRobotActionUpdate = getGreedyRobotAction(state, null);
				//Main.connect.sendMessage("for sugg human "+bestHumanAction+" robot "+bestRobotActionSuggestion+" for update "+bestRobotActionUpdate);
				for(Action humanAction : humanActions){
					double value = getJointQValue(state, new HumanRobotActionPair(humanAction, bestRobotActionUpdate));
					cumulativeValue += value;
				}
			}
			double averageValue = cumulativeValue/humanActions.length;
			
			//connect.sendMessage("sugg "+maxJointValue+" average "+averageValue);
			if((maxJointValue - averageValue) > THRESHOLD_SUGG && bestHumanAction != null){ //robot suggests human an action too
				numRobotSuggestions++;
				actions = new HumanRobotActionPair(bestHumanAction, bestRobotActionSuggestion);
				enableSend(false);
				connect.sendMessage("Waiting for teammate...\n");
				simulateWaitTime(state);
				connect.sendMessage("Your teammate will choose to "+getPrintableFromAction(bestRobotActionSuggestion)+" and suggests you to "+getPrintableFromAction(bestHumanAction));
				enableSend(true);
				connect.sendMessage("Would you like to accept the suggestion? (Enter Y or N)");
				startTimer();
				String entered = connect.getMessage().trim().toUpperCase();
				while(!isValidResponseToSuggestion(entered)){
					connect.sendMessage("Sorry that was an invalid response, please enter again: ");
					entered = connect.getMessage().trim().toUpperCase();
				}
				if(entered.equals("N")){
					numHumanRejects++;
					connect.sendMessage("Please enter which fire you would like to extinguish instead (A, B, C, D, E).");
					entered = connect.getMessage().trim().toUpperCase();
					Action humanEnteredAction = getActionFromStr(entered, mdp.humanAgent.actionsAsList(state));
					if(humanEnteredAction == null || !mdp.humanAgent.actionsAsList(state).contains(humanEnteredAction)){
						if(humanEnteredAction != Action.WAIT)
							humanEnteredAction = getCorrectedResponse(mdp.humanAgent.actionsAsList(state));
					}
					if(humanEnteredAction == Action.WAIT){
						connect.sendMessage("\nSorry you ran out of time!\nSummary:\n"
								+ "Your teammate chose to "+getPrintableFromAction(bestRobotActionSuggestion));
						return new HumanRobotActionPair(Action.WAIT, bestRobotActionSuggestion);
					}
					connect.sendMessage("\nSummary:\nYou chose to "+getPrintableFromAction(humanEnteredAction)+"\nYour teammate chose to "+getPrintableFromAction(bestRobotActionSuggestion)+"\n");
					return new HumanRobotActionPair(humanEnteredAction, bestRobotActionSuggestion);
				} else if(entered.equals("Y")){
					numHumanAccepts++;
					connect.sendMessage("\nSummary:\nYou chose to "+getPrintableFromAction(actions.getHumanAction())+"\nYour teammate chose to "+getPrintableFromAction(actions.getRobotAction())+"\n");
					return actions;
				} else {
					connect.sendMessage("\nSorry you ran out of time!\nSummary:\n"
							+ "Your teammate chose to "+getPrintableFromAction(bestRobotActionSuggestion));
					return new HumanRobotActionPair(Action.WAIT, bestRobotActionSuggestion);
				}
			} else { //robot just updates
				numRobotUpdates++;
				enableSend(false);
				connect.sendMessage("Waiting for teammate...\n");
				simulateWaitTime(state);
				connect.sendMessage("Your teammate will "+getPrintableFromAction(bestRobotActionUpdate));
				enableSend(true);
				connect.sendMessage("Please enter which fire you would like to extinguish (A, B, C, D, E).");
				startTimer();
				String entered = connect.getMessage().trim().toUpperCase();
				Action humanEnteredAction = getActionFromStr(entered, mdp.humanAgent.actionsAsList(state));
				if(humanEnteredAction == null || !mdp.humanAgent.actionsAsList(state).contains(humanEnteredAction)){
					if(humanEnteredAction != Action.WAIT)
						humanEnteredAction = getCorrectedResponse(mdp.humanAgent.actionsAsList(state));
				}
				if(humanEnteredAction == Action.WAIT){
					connect.sendMessage("\nSorry you ran out of time!\nSummary:\n"
							+ "Your teammate chose to "+getPrintableFromAction(bestRobotActionUpdate));
					return new HumanRobotActionPair(Action.WAIT, bestRobotActionUpdate);
				}
				connect.sendMessage("\nSummary:\nYou chose to "+getPrintableFromAction(humanEnteredAction)+"\nYour teammate chose to "+getPrintableFromAction(bestRobotActionUpdate)+"\n");
				return new HumanRobotActionPair(humanEnteredAction, bestRobotActionUpdate);
			}
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
			Action humanAction;
			Action robotAction;
			connect.sendMessage("Please enter the fire you would like to extinguish (A, B, C, D, E) or \'BOTH\' for specifying the fires for both you and your teammate: ");
			enableSend(true);
			startTimer();
			String entered = connect.getMessage().trim().toUpperCase();
			while(!isValid(entered, mdp.humanAgent.actionsAsList(state))){
				connect.sendMessage("Sorry that was an invalid response, please enter again: ");
				entered = connect.getMessage().trim().toUpperCase();
			}
			if(entered.equalsIgnoreCase("BOTH")){
				numHumanSuggestions++;
				connect.sendMessage("Please enter first which fire YOU want to extinguish (A, B, C, D, E): ");
				entered = connect.getMessage().trim().toUpperCase();
				humanAction = getActionFromStr(entered, mdp.humanAgent.actionsAsList(state));
				if(humanAction == null || !mdp.humanAgent.actionsAsList(state).contains(humanAction)){
					if(humanAction != Action.WAIT)
						humanAction = getCorrectedResponse(mdp.humanAgent.actionsAsList(state));
				}
				
				connect.sendMessage("Please enter which fire you would like YOUR TEAMMATE to extinguish (A, B, C, D, E): ");
				entered = connect.getMessage().trim().toUpperCase();
				robotAction = getActionFromStr(entered, mdp.robotAgent.actionsAsList(state));
				if(robotAction == null || !mdp.robotAgent.actionsAsList(state).contains(robotAction)){
					if(robotAction != Action.WAIT)
						robotAction = getCorrectedResponse(mdp.robotAgent.actionsAsList(state));
				}
				
				if(humanAction == Action.WAIT || robotAction == Action.WAIT){
					connect.sendMessage("\nSorry you ran out of time!\nSummary:\n"
							+ "Both you and your teammate have to wait this turn.");
					return new HumanRobotActionPair(Action.WAIT, Action.WAIT);
				}
				double humanSuggestedQValue = getJointQValue(state, new HumanRobotActionPair(humanAction, robotAction));
				Action optimalRobotAction = null;
				if(pastRobotAction != null)
					optimalRobotAction = pastRobotAction;
				else
					optimalRobotAction = getGreedyRobotAction(state, humanAction);
				double robotSuggestedQValue = getJointQValue(state, new HumanRobotActionPair(humanAction, optimalRobotAction));
				enableSend(false);
				stopTimer();
				connect.sendMessage("Waiting for teammate...");
				simulateWaitTime(state);
				//connect.sendMessage("robotvalue "+robotSuggestedQValue+" humanvalue "+humanSuggestedQValue);
				if((robotSuggestedQValue - humanSuggestedQValue) > THRESHOLD_REJECT){ //robot rejects human suggestion and chooses own action assuming human will do their suggested action
					numRobotRejects++;
					connect.sendMessage("Your teammate has a different preference and chooses to "+getPrintableFromAction(optimalRobotAction));//+" robotValue "+robotSuggestedQValue+" humanValue "+humanSuggestedQValue);
					robotAction = optimalRobotAction;
				} else {
					numRobotAccepts++;
					connect.sendMessage("Your teammate accepts to "+getPrintableFromAction(robotAction));// robotValue "+robotSuggestedQValue+" humanValue "+humanSuggestedQValue);
				}
			} else {
				numHumanUpdates++;
				humanAction = getActionFromStr(entered, mdp.humanAgent.actionsAsList(state));
				if(humanAction == null || !mdp.humanAgent.actionsAsList(state).contains(humanAction)){
					if(humanAction != Action.WAIT)
						humanAction = getCorrectedResponse(mdp.humanAgent.actionsAsList(state));
				}
				if(humanAction == Action.WAIT){
					connect.sendMessage("\nSorry you ran out of time!\nSummary:\n"
							+ "Both you and your teammate have to wait this turn.");
					return new HumanRobotActionPair(Action.WAIT, Action.WAIT);
				}
				enableSend(false);
				stopTimer();
				connect.sendMessage("Waiting for teammate...");
				simulateWaitTime(state);
				robotAction = getGreedyRobotAction(state, humanAction);
			}
			connect.sendMessage("\nSummary:\nYou chose to "+getPrintableFromAction(humanAction)+"\nYour teammate chose to "+getPrintableFromAction(robotAction)+"\n");
			return new HumanRobotActionPair(humanAction, robotAction);
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Adds wait time to simulate a human playing
	 */
	public void simulateWaitTime(State state) {
		int stateScore = 0;
		for(int i=0; i<state.stateOfFires.length; i++){
			int num = state.stateOfFires[i];
			if(num == MyWorld.BURNOUT)
				stateScore += 0;
			else
				stateScore += num;
		}
		System.out.println("score "+stateScore);
		try{
			if(stateScore < 10){
				int shortRandomTime = 6; //Main.rand.nextInt(3)+5;
				System.out.println(shortRandomTime*1000);
				Thread.sleep(shortRandomTime*1000);
			} else {
				int longRandomTime = 8; //Main.rand.nextInt(5)+8;
				System.out.println(longRandomTime*1000);
				Thread.sleep(longRandomTime*1000);
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
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
	
	public static void enableNextButton() {
		Main.st.server.nextButton.setEnabled(true);
		Main.st.server.sendField.setEnabled(false);
		Main.st.server.sendButton.setEnabled(false);
	}
	
	public static void waitForClick() {
		while(!Main.st.server.nextClicked){
			System.out.print("");
        }
		Main.st.server.nextButton.setEnabled(false);
		Main.st.server.sendField.setEnabled(true);
		Main.st.server.sendButton.setEnabled(true);
		Main.st.server.nextClicked = false;
	}
	
	public static void enableSend(boolean enable) {
		Main.st.server.sendField.setEnabled(enable);
		Main.st.server.sendButton.setEnabled(enable);
		Main.st.server.sendField.requestFocus();
	}
	
	public static void disableNext(){
		Main.st.server.nextButton.setEnabled(false);
	}
	
	public void startTimer(){
		timeLeft = Constants.MAX_TIME;
	    Main.st.server.timeDisplay.setText(""+timeLeft);
	    timer.start();
	}
	
	public void stopTimer(){
		Main.st.server.timeDisplay.setText("");
		timer.stop();
		timeLeft = Constants.MAX_TIME;
	}
	
	public ActionListener timerListener() {
		return new ActionListener() {
		  public void actionPerformed(ActionEvent evt) {
			  timeLeft--;
		      Main.st.server.timeDisplay.setText(""+timeLeft);
		      if(timeLeft == 0){
		    	  timer.stop();
		      }
		  }
		};
	}
}
