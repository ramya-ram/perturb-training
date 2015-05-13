package code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyWorld {
	public static MDP mdp;
	public static List<State> states = new ArrayList<State>();
	public static State[] initStates;
	public String predefinedText;
	public String textToDisplay;
	
	//public Location goalLoc;
	
	public List<Location> staticTokenLocs;
	public List<Location> staticPitLocs;

	public List<Location> currTokenLocs;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum){//, Location goalLoc, List<Location> tokenLocs, List<Location> pitLocs){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		//if(typeOfWorld == Constants.TRAINING)
		//this.goalLoc = goalLoc.clone();
		//else if(typeOfWorld == Constants.TESTING)
			//this.goalLoc = new Location(Tools.rand.nextInt(Constants.NUM_ROWS), Tools.rand.nextInt(Constants.NUM_COLS));
		
		//this.tokenLocs = new ArrayList<Location>();
		//this.tokenLocs.addAll(tokenLocs);
		//this.pitLocs = new ArrayList<Location>();
		//this.pitLocs.addAll(pitLocs);
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
		
		//System.out.println("testWind="+testWind+" testDryness="+testDryness+" simulationWind="+simulationWind+" simulationDryness="+simulationDryness);
	}
	
	/*public void changeGoalLoc(){
		this.goalLoc = new Location(Tools.rand.nextInt(Constants.NUM_ROWS), Tools.rand.nextInt(Constants.NUM_COLS));
	}*/
	
	public void changeTokenPitLocs(){
		this.staticTokenLocs = new ArrayList<Location>();
		for(int i=0; i<12; i++)
			this.staticTokenLocs.add(new Location(Tools.rand.nextInt(Constants.NUM_ROWS), Tools.rand.nextInt(Constants.NUM_COLS)));
		this.staticPitLocs = new ArrayList<Location>();
		for(int i=0; i<12; i++)
			this.staticPitLocs.add(new Location(Tools.rand.nextInt(Constants.NUM_ROWS), Tools.rand.nextInt(Constants.NUM_COLS)));
	}
	
	public void resetTokenLocs(){
		currTokenLocs = new ArrayList<Location>();
		currTokenLocs.addAll(staticTokenLocs);
		//pitLocs.clear();
		/*if(typeOfWorld == Constants.TRAINING){
			tokenLocs.addAll(Constants.allTokenLocs.get(sessionNum-1));
			pitLocs.addAll(Constants.allPitLocs.get(sessionNum-1));
		} else if(typeOfWorld == Constants.TESTING){
			tokenLocs.addAll(Constants.allTokenLocsTest.get(sessionNum-1));
			pitLocs.addAll(Constants.allPitLocsTest.get(sessionNum-1));
		}*/	
	}
	
	public static State getStateFromFile(String str){
		/*int[] statesOfFires = new int[Constants.NUM_FIRES];
		for(int i=0; i<str.length(); i++)
			statesOfFires[i] = str.charAt(i)-'0';
		return new State(statesOfFires);*/
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
		//int numStates = (int) Math.pow(statesPerFire - 1, Constants.NUM_FIRES);
		initStates = new State[states.size()];
		int count=0;
		for(State state : states){
			//if(state.noItemsInState(Constants.BURNOUT)){
			initStates[count] = state.clone();
			count++;
			//}
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
				/*if(s.robotLoc.equals(goalLoc)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}*/
				if(s.robotLoc.row>0)
					possibleActions.add(Action.UP);
				if(s.robotLoc.row<Constants.NUM_ROWS-1)
					possibleActions.add(Action.DOWN);
				if(s.robotLoc.col>0)
					possibleActions.add(Action.LEFT);
				if(s.robotLoc.col<Constants.NUM_COLS-1)
					possibleActions.add(Action.RIGHT);
				possibleActions.add(Action.WAIT);
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
				/*if(s.humanLoc.equals(goalLoc)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}*/
				if(s.humanLoc.row>0)
					possibleActions.add(Action.UP);
				if(s.humanLoc.row<Constants.NUM_ROWS-1)
					possibleActions.add(Action.DOWN);
				if(s.humanLoc.col>0)
					possibleActions.add(Action.LEFT);
				if(s.humanLoc.col<Constants.NUM_COLS-1)
					possibleActions.add(Action.RIGHT);
				possibleActions.add(Action.WAIT);
				return possibleActions;
			}	
		};
	}
	
	public boolean isGoalState(State state){
		return currTokenLocs.isEmpty();//state.humanLoc.equals(goalLoc) && state.robotLoc.equals(goalLoc);
	}
	
	public State initialState(){
		if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			return new State(new Location(Constants.NUM_ROWS-1,0), new Location(0,Constants.NUM_COLS-1));
		}
		return initStates[Tools.rand.nextInt(initStates.length)];	
	}
	
	/**
	 * Computes reward for being in this state
	 * Reward is given based on the intensities of fires, burnouts get high negative reward, even after the goal is reached 
	 */
	public double reward(State state, HumanRobotActionPair agentActions, State nextState){		
		/*if(isGoalState(nextState))
			return -(100*nextState.getNumItemsInState(Constants.BURNOUT));
		double reward = -10;
		for(int i=0; i<nextState.stateOfFires.length; i++){
			reward += -1*nextState.stateOfFires[i];
		}*/
		//if(isGoalState(nextState))
			//return 50;
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
		return getStateFromFile(Main.proceTestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()]);
	}
	
	/**
	 * Computes the next state and prints appropriate messages on SocketTest based on saved predefined case from file
	 */
	public State getPredefinedNextState(State state, HumanRobotActionPair agentActions){
		try{
			textToDisplay = "";
			System.out.println("USING PREDEFINED");
			State nextState = getProcePredefinedNextState(state, agentActions).clone();
			textToDisplay += "State after your actions: "+nextState.toString()+"\n";
			if(Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
			
			if(sessionNum == 1 && sessionNum == 2){ //base task
				return nextState;  //TODO: change if 1st test task is not procedural (no wind/dryness)
			} else {		
				String text = "";
				if(typeOfWorld == Constants.TESTING){
					if(sessionNum == 3){
						String str = Main.perturb1TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	
					} else if(sessionNum == 4){
						String str = Main.perturb2TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	 
					} 
				}
				for(int i=0; i<text.length(); i+=3){
					if(text.length()<=1)
						break;
					System.out.println("str causing error "+text);
					String str = text.substring(i, i+3);
					if(str.charAt(0) == 'B'){
						int fire = str.charAt(1)-48;
						textToDisplay += getBurnoutMessage(fire)+"\n";
						//nextState = getNextStateAfterBurnout(nextState, fire);
					} else if(str.charAt(0) == 'S'){
						int spreadTo = str.charAt(2)-48;
						textToDisplay += getSpreadMessage(str.charAt(1)-48, spreadTo)+"\n";
						//nextState = getNextStateAfterSpread(nextState, spreadTo);
					}
				}
			}
			textToDisplay += "Final state: "+nextState.toString();
			if(Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
			return nextState;
		} catch(Exception e){
			e.printStackTrace();
		}
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
	
	/**
	 * Converts an index to the corresponding letter
	 */
	public static String convertToFireName(int index){
		switch(index){
			case 0:
				return "Alpha";
			case 1:
				return "Bravo";
			case 2:
				return "Charlie";
			case 3:
				return "Delta";
			case 4:
				return "Echo";		
		}
		return "None";
	}
	
	public String getSpreadMessage(int spreadFrom, int spreadTo){
		return "Fire "+convertToFireName(spreadFrom)+
				" spread to fire "+convertToFireName(spreadTo)+" because of wind!!";
	}
	
	public String getBurnoutMessage(int fire){
		return "Fire "+convertToFireName(fire)+" burned down the building because of dryness!!";
	}
	
	public boolean checkIfValidFireLoc(int index, int[] stateOfFires) {
		return index >= 0;// && index < Constants.NUM_FIRES && stateOfFires[index] < Constants.HIGHEST;
	}
}