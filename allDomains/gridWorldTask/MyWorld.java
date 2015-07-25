package code;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import code.Action;
import code.Constants;
import code.State;

/**
 * Implementation for the domain specifics of an MDP
 * Specifies the transition function, reward function, etc of the world/MDP
 */
public class MyWorld {
	public static MDP mdp;
	public static List<State> states = new ArrayList<State>();
	public static State[] initStates;
	public String predefinedText;
	public String textToDisplay;
	public String fileName;
	
	public Location goalLoc; //goal location in the grid
	
	//token locations give the agents additional reward if picked up
	public static List<Location> staticTokenLocs = Arrays.asList(
			new Location(4,1), new Location(4,2), new Location(4,3), new Location(5,6), new Location(5,7), new Location(5,8), 
			new Location(1,5), new Location(2,5), new Location(3,5), new Location(6,4), new Location(7,4), new Location(8,4));
	//pit locations give agents negative reward when crossed
	public static List<Location> staticPitLocs = Arrays.asList(
			new Location(1,1), new Location(2,2), new Location(3,3), new Location(6,6), new Location(7,7), new Location(8,8), 
			new Location(8,1), new Location(7,2), new Location(6,3), new Location(3,6), new Location(2,7), new Location(1,8));

	//keeps track of the tokens in each episode that have not yet been picked up (tokens are gone when picked up once)
	public List<Location> currTokenLocs;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum, Location goalLoc){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		this.goalLoc = goalLoc;
		
		String type = "";
		if(typeOfWorld == Constants.TRAINING)
			type = "train";
		else if(typeOfWorld == Constants.TESTING)
			type = "test";
				
		fileName = Constants.simulationDir+type+"world_"+Constants.DOMAIN_NAME+"_"+goalLoc.row+"_"+goalLoc.col+".csv";
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
	}
	
	/**
	 * Prints the grid with tokens and pits for visualization
	 */
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
	
	/**
	 * Chooses a random goal location
	 */
	public void changeGoalLoc(){
		this.goalLoc = new Location(Constants.rand.nextInt(Constants.NUM_ROWS), Constants.rand.nextInt(Constants.NUM_COLS));
		System.out.println("Goal Location: "+this.goalLoc);
	}
	
	/**
	 * This method can be used to change any variables in the task for each new episode
	 * Here, we reset all token locations each time the agent begins the task again
	 */
	public void reset(){
		currTokenLocs = new ArrayList<Location>();
		currTokenLocs.addAll(staticTokenLocs);	
	}
	
	/**
	 * A string read from a file is converted to a state
	 */
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
						//for all possible locations for the human and robot, create a new state and add it to the list of states
						Location robotLoc = new Location(row2, col2);
						State state = new State(humanLoc, robotLoc);
						states.add(state);
					}
				}
			}
		}
		//in this task, all states can be initial states
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
				//the robot can move up, down, left, and right according to its current location on the grid
				if(s.robotLoc.row>0)
					possibleActions.add(Action.UP);
				if(s.robotLoc.row<Constants.NUM_ROWS-1)
					possibleActions.add(Action.DOWN);
				if(s.robotLoc.col>0)
					possibleActions.add(Action.LEFT);
				if(s.robotLoc.col<Constants.NUM_COLS-1)
					possibleActions.add(Action.RIGHT);
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
				//the human can move up, down, left, and right according to its current location on the grid
				if(s.humanLoc.row>0)
					possibleActions.add(Action.UP);
				if(s.humanLoc.row<Constants.NUM_ROWS-1)
					possibleActions.add(Action.DOWN);
				if(s.humanLoc.col>0)
					possibleActions.add(Action.LEFT);
				if(s.humanLoc.col<Constants.NUM_COLS-1)
					possibleActions.add(Action.RIGHT);
				return possibleActions;
			}	
		};
	}
	
	/**
	 * Determines if the given state is a goal state
	 * Here, the goal state is when the human and robot reaches the goal location
	 */
	public boolean isGoalState(State state){
		return state.humanLoc.equals(goalLoc) && state.robotLoc.equals(goalLoc);
	}
	
	/**
	 * Returns an initial state for the episode (either randomly from the initStates array or a specific initial state)
	 */
	public State initialState(){
		if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			return new State(new Location(4, 0), new Location(4, Constants.NUM_COLS-1));
		}
		return initStates[Constants.rand.nextInt(initStates.length)];	
	}
	
	/**
	 * Computes reward for being in this state
	 * Reward is given based on the intensities of fires, burnouts get high negative reward, even after the goal is reached 
	 */
	public double reward(State state, HumanRobotActionPair agentActions, State nextState){
		if(isGoalState(nextState))
			//+20 is given when the goal state is reached
			return 20;
		double reward = -1;
		//if both the human and robot get the token together, +5 is given ()
		if(nextState.humanLoc.equals(nextState.robotLoc) && currTokenLocs.contains(nextState.humanLoc)){
			reward += 5;
			currTokenLocs.remove(nextState.humanLoc);
		}
		//+1 is given if either the human OR the robot gets a token (so it's better to coordinate and get it together)
		if(currTokenLocs.contains(nextState.humanLoc)){
			reward += 1;
			currTokenLocs.remove(nextState.humanLoc);
		}
		if(currTokenLocs.contains(nextState.robotLoc)){
			reward += 1;
			currTokenLocs.remove(nextState.robotLoc);
		}
		//-5 is given every time the human or robot are on a pit location
		if(staticPitLocs.contains(nextState.humanLoc))
			reward -= 5;
		if(staticPitLocs.contains(nextState.robotLoc))
			reward -= 5;
		return reward;
	}
	
	/**
	 * Determines the next state and prints appropriate messages
	 * There can be stochasticity in robot movement
	 */
	public State getNextState(State state, HumanRobotActionPair agentActions){
		textToDisplay = "";
		predefinedText = "";
		
		State newState = state.clone();
		if(isGoalState(newState))
			return newState;

		try{	
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			
			//gets the next state locations given the human and robot actions
			newState.humanLoc = getNextStateLoc(newState.humanLoc, humanAction);
			newState.robotLoc = getNextStateLoc(newState.robotLoc, robotAction);
			
			if(Main.currWithSimulatedHuman && Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
		} catch(Exception e){
			e.printStackTrace();
		}
		return newState;
	}
	
	/**
	 * Gets the next state location given the current location and action
	 * Stochasticity is added by including uncertainty in the action
	 * 80% of the time, the agent will go to the desired location
	 * 10% of the time, it will go to the left and the last 10%, it will go to the right
	 */
	public Location getNextStateLoc(Location currLoc, Action action){
		Location newLoc = currLoc.clone();
		int randNum = Constants.rand.nextInt(100);
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