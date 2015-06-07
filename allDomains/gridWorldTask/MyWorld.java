package code;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import code.Action;
import code.State;

public class MyWorld {
	public static MDP mdp;
	public static List<State> states = new ArrayList<State>();
	public static State[] initStates;
	public String predefinedText;
	public String textToDisplay;
	
	public Location goalLoc;
	
	public static List<Location> staticTokenLocs = Arrays.asList(
			new Location(4,1), new Location(4,2), new Location(4,3), new Location(5,6), new Location(5,7), new Location(5,8), 
			new Location(1,5), new Location(2,5), new Location(3,5), new Location(6,4), new Location(7,4), new Location(8,4));
	public static List<Location> staticPitLocs = Arrays.asList(
			new Location(1,1), new Location(2,2), new Location(3,3), new Location(6,6), new Location(7,7), new Location(8,8), 
			new Location(8,1), new Location(7,2), new Location(6,3), new Location(3,6), new Location(2,7), new Location(1,8));

	public List<Location> currTokenLocs;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum, Location goalLoc){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		this.goalLoc = goalLoc;
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
	}
	
	public void printGrid(){
		for(int i=0; i<Constants.NUM_ROWS; i++){
			for(int j=0; j<Constants.NUM_COLS; j++){
				if(staticTokenLocs.contains(new Location(i,j)))
					System.out.print("O");
				else if(staticPitLocs.contains(new Location(i,j)))
					System.out.print("X");
				else
					System.out.print(" ");
			}
			System.out.println();
		}
	}
	
	public void changeGoalLoc(){
		this.goalLoc = new Location(Tools.rand.nextInt(Constants.NUM_ROWS), Tools.rand.nextInt(Constants.NUM_COLS));
		System.out.println("Goal Location: "+this.goalLoc);
	}
	
	public void reset(){
		currTokenLocs = new ArrayList<Location>();
		currTokenLocs.addAll(staticTokenLocs);	
	}
	
	public static State getStateFromFile(String str){
		return null;
	}
	
	/**
	 * Initializes the MDP for the particular task by specifying the set of states, the action function, the reward function, etc
	 */
	public MDP initializeMDP() {
		if(states == null || initStates == null)
			initStates();
		System.out.println("Init states "+states.size());
		ActionsFunction humanActionsFunction = initHumanActionsFunction();
		ActionsFunction robotActionsFunction = initRobotActionsFunction();	
		RobotAgent robot = new RobotAgent(robotActionsFunction);
		HumanAgent human = new HumanAgent(humanActionsFunction);
		return new MDP(states, human, robot);
	}

	/**
	 * Initializes set of states
	 */
	public void initStates() {
		for(int row1 = 0; row1 < Constants.NUM_ROWS; row1++){
			for(int col1 = 0; col1 < Constants.NUM_COLS; col1++){
				Location humanLoc = new Location(row1, col1);
				for(int row2 = 0; row2 < Constants.NUM_ROWS; row2++){
					for(int col2 = 0; col2 < Constants.NUM_COLS; col2++){
						Location robotLoc = new Location(row2, col2);
						State state = new State(humanLoc, robotLoc);
						states.add(state);
					}
				}
			}
		}
		initStates = new State[states.size()];
		int count=0;
		for(State state : states){
			initStates[count] = state.clone();
			count++;
		}
		System.out.println("init states size "+count);
	}
	
	/**
	 * Initializes the action function that specifies the possible set of actions from each state
	 */
	public ActionsFunction initRobotActionsFunction() {
		return new ActionsFunction() {
			@Override
			public Set<Action> actions(State s) {
				Set<Action> possibleActions = new HashSet<Action>();
				if(s.robotLoc.equals(goalLoc)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}
				if(s.robotLoc.row>0)
					possibleActions.add(Action.UP);
				if(s.robotLoc.row<Constants.NUM_ROWS-1)
					possibleActions.add(Action.DOWN);
				if(s.robotLoc.col>0)
					possibleActions.add(Action.LEFT);
				if(s.robotLoc.col<Constants.NUM_COLS-1)
					possibleActions.add(Action.RIGHT);
				//possibleActions.add(Action.WAIT);
				return possibleActions;
			}	
		};
	}
	
	/**
	 * Initializes the action function that specifies the possible set of actions from each state
	 */
	public ActionsFunction initHumanActionsFunction() {
		return new ActionsFunction() {
			@Override
			public Set<Action> actions(State s) {
				Set<Action> possibleActions = new HashSet<Action>();
				if(s.humanLoc.equals(goalLoc)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}
				if(s.humanLoc.row>0)
					possibleActions.add(Action.UP);
				if(s.humanLoc.row<Constants.NUM_ROWS-1)
					possibleActions.add(Action.DOWN);
				if(s.humanLoc.col>0)
					possibleActions.add(Action.LEFT);
				if(s.humanLoc.col<Constants.NUM_COLS-1)
					possibleActions.add(Action.RIGHT);
				//possibleActions.add(Action.WAIT);
				return possibleActions;
			}	
		};
	}
	
	public boolean isGoalState(State state){
		return state.humanLoc.equals(goalLoc) && state.robotLoc.equals(goalLoc);
	}
	
	public State initialState(){
		if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			return new State(new Location(4, 0), new Location(4, Constants.NUM_COLS-1));
		}
		return initStates[Tools.rand.nextInt(initStates.length)];	
	}
	
	/**
	 * Computes reward for being in this state
	 * Reward is given based on the intensities of fires, burnouts get high negative reward, even after the goal is reached 
	 */
	public double reward(State state, HumanRobotActionPair agentActions, State nextState){
		if(isGoalState(nextState))
			return 20;
		double reward = -1;
		if(nextState.humanLoc.equals(nextState.robotLoc) && currTokenLocs.contains(nextState.humanLoc)){
			reward += 5;
			currTokenLocs.remove(nextState.humanLoc);
		}
		if(currTokenLocs.contains(nextState.humanLoc)){
			reward += 1;
			currTokenLocs.remove(nextState.humanLoc);
		}
		if(currTokenLocs.contains(nextState.robotLoc)){
			reward += 1;
			currTokenLocs.remove(nextState.robotLoc);
		}
		if(staticPitLocs.contains(nextState.humanLoc))
			reward -= 5;
		if(staticPitLocs.contains(nextState.robotLoc))
			reward -= 5;
		return reward;
	}
	
	public State getProcePredefinedNextState(State state, HumanRobotActionPair agentActions) {
		return null;
	}
	
	/**
	 * Computes the next state and prints appropriate messages on SocketTest based on saved predefined case from file
	 */
	public State getPredefinedNextState(State state, HumanRobotActionPair agentActions){
		return null;
	}
	
	/**
	 * Determines the next state and prints appropriate messages to SocketTest
	 * There can be stochasticity through spreading and burnout of fires
	 */
	public State getNextState(State state, HumanRobotActionPair agentActions){
		textToDisplay = "";
		predefinedText = "";
		
		State newState = state.clone();
		if(isGoalState(newState))
			return newState;

		try{
			if(Main.currWithSimulatedHuman){
				if(Constants.usePredefinedTestCases && typeOfWorld == Constants.TESTING){
					newState = getPredefinedNextState(newState, agentActions);
					return newState;
				}
			}
			
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			
			newState.humanLoc = getNextStateLoc(newState.humanLoc, humanAction);
			newState.robotLoc = getNextStateLoc(newState.robotLoc, robotAction);
			
			if(Main.currWithSimulatedHuman && Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
		} catch(Exception e){
			e.printStackTrace();
		}
		return newState;
	}
	
	public Location getNextStateLoc(Location currLoc, Action action){
		Location newLoc = currLoc.clone();
		int randNum = Tools.rand.nextInt(100);
		switch(action){
			case UP:
				if(randNum < 80) {
					newLoc.row--;
				} else if(randNum < 90) {
					if(currLoc.col > 0)
						newLoc.col--;
				} else {
					if(currLoc.col < Constants.NUM_COLS-1)
						newLoc.col++;
				}
				break;
			case DOWN:
				if(randNum < 80) {
					newLoc.row++;
				} else if(randNum < 90) {
					if(currLoc.col > 0)
						newLoc.col--;
				} else {
					if(currLoc.col < Constants.NUM_COLS-1)
						newLoc.col++;
				}
				break;
			case LEFT:
				if(randNum < 80) {
					newLoc.col--;
				} else if(randNum < 90) {
					if(currLoc.row > 0)
						newLoc.row--;
				} else {
					if(currLoc.row < Constants.NUM_ROWS-1)
						newLoc.row++;
				}
				break;
			case RIGHT:
				if(randNum < 80) {
					newLoc.col++;
				} else if(randNum < 90) {
					if(currLoc.row > 0)
						newLoc.row--;
				} else {
					if(currLoc.row < Constants.NUM_ROWS-1)
						newLoc.row++;
				}
				break;
			case WAIT:
		}
		return newLoc;
	}
	
	public String getPrintableFromAction(Action action){
		return "";
	}
	
	public State initialState(int roundNum){
		return null;
	}
	
	public void setTitleLabel(int roundNum, Color[] colorArray, int indexOfColor){
		return;
	}
	
	public static void initForExperiments(List<MyWorld> trainingWorldsProce, List<MyWorld> trainingWorldsPerturb, List<MyWorld> testingWorlds){
		return;
	}
	
	public void simulateWaitTime(State state) {
		return;
	}
	
	public static Action getActionFromInput(String str){
		return null;
	}
	
    public static void updateState(State state) {
    	return;
    }
}