package code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyWorld {
	public static MDP mdp;
	public static List<State> states = new ArrayList<State>();
	public static State[] initStates;
	public String predefinedText;
	public String textToDisplay;
	
	public static List<Location> startLocs = Arrays.asList(new Location(0,0), new Location(1,0), new Location(2,0));
	public static List<Location> endLocs = Arrays.asList(new Location(0,2), new Location(1,2), new Location(2,2));
	
	public int[] queueItems;
	public int obstacleRow;
	
	public int[] staticGoalItems;
	public int[] currItemsLeft;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum, int[] staticGoalItems){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		this.staticGoalItems = staticGoalItems;
		
		queueItems = new int[Constants.NUM_ITEMS];
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
	}
	
	public void resetGoalItems(){
		currItemsLeft = new int[staticGoalItems.length];
		for(int i=0; i<currItemsLeft.length; i++)
			currItemsLeft[i] = staticGoalItems[i];
	}
	
	/*public void printGrid(){
		for(int i=0; i<Constants.NUM_ROWS; i++){
			for(int j=0; j<Constants.NUM_COLS; j++){
				
			}
			System.out.println();
		}
	}*/
	
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
						for(int humanItem = -1; humanItem < Constants.NUM_ITEMS; humanItem++){
							for(int robotItem = -1; robotItem < Constants.NUM_ITEMS; robotItem++){
								//for(int obsRow = 0; obsRow < Constants.NUM_ROWS; obsRow++){
									//Location obsLoc = new Location(obsRow, 1); //obstacle only stays in the middle column, goes up and down this column
									State state = new State(humanLoc, robotLoc, humanItem, robotItem);//, obsRow);
									states.add(state);
								//}
							}
						}
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
				//if(s.robotLoc.equals(goalLoc)){
				//	possibleActions.add(Action.WAIT);
				//	return possibleActions;
				//}
				if(endLocs.contains(s.robotLoc) && s.robotItem == s.robotLoc.row)
					possibleActions.add(Action.DROP_OFF);
				else {
					if(startLocs.contains(s.robotLoc) && s.robotItem == -1){
						int currentItem = queueItems[s.robotLoc.row];
						if(currItemsLeft[currentItem]>0)
							possibleActions.add(Action.PICK_UP);
					}
					if(s.robotLoc.row>0)
						possibleActions.add(Action.UP);
					if(s.robotLoc.row<Constants.NUM_ROWS-1)
						possibleActions.add(Action.DOWN);
					if(s.robotLoc.col>0)
						possibleActions.add(Action.LEFT);
					if(s.robotLoc.col<Constants.NUM_COLS-1)
						possibleActions.add(Action.RIGHT);
				}
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
				//if(s.humanLoc.equals(goalLoc)){
				//	possibleActions.add(Action.WAIT);
				//	return possibleActions;
				//}
				if(endLocs.contains(s.humanLoc) && s.humanItem == s.humanLoc.row)
					possibleActions.add(Action.DROP_OFF);
				else{
					if(startLocs.contains(s.humanLoc) && s.humanItem == -1){
						int currentItem = queueItems[s.humanLoc.row];
						if(currItemsLeft[currentItem]>0)
							possibleActions.add(Action.PICK_UP);
					}
					if(s.humanLoc.row>0)
						possibleActions.add(Action.UP);
					if(s.humanLoc.row<Constants.NUM_ROWS-1)
						possibleActions.add(Action.DOWN);
					if(s.humanLoc.col>0)
						possibleActions.add(Action.LEFT);
					if(s.humanLoc.col<Constants.NUM_COLS-1)
						possibleActions.add(Action.RIGHT);
				}
				//possibleActions.add(Action.WAIT);
				return possibleActions;
			}	
		};
	}
	
	public boolean isGoalState(State state){
		//currTokenLocs.isEmpty();
		//return state.humanLoc.equals(goalLoc) && state.robotLoc.equals(goalLoc);
		//return false;
		for(int i=0; i<currItemsLeft.length; i++){
			if(currItemsLeft[i] > 0)
				return false;
		}
		return true;
	}
	
	public State initialState(){
		//if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			return new State(new Location(0, 0), new Location(Constants.NUM_ROWS-1, 0), -1, -1);//, 1);
		//}
		//return initStates[Tools.rand.nextInt(initStates.length)];	
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
	 * Computes reward for being in this state
	 * Reward is given based on the intensities of fires, burnouts get high negative reward, even after the goal is reached 
	 */
	public double reward(State state, HumanRobotActionPair agentActions, State nextState){		
		double reward = -1;
		Action humanAction = agentActions.getHumanAction();
		Action robotAction = agentActions.getRobotAction();
		if(humanAction == Action.DROP_OFF){
			reward += getDropOffReward(queueItems[state.humanLoc.row]);
		}
		if(robotAction == Action.DROP_OFF){
			reward += getDropOffReward(queueItems[state.robotLoc.row]);
		}
		if(state.humanLoc.equals(new Location(obstacleRow, Constants.OBSTACLE_COL)))
			reward += -5;
		if(state.robotLoc.equals(new Location(obstacleRow, Constants.OBSTACLE_COL)))
			reward += -5;
		return reward;
	}
	
	public double getDropOffReward(int row){
		/*int[][] trainingReward = {{1,3,5},
								  {1,3,5},
								  {3,3,3}};
		int[][] testingReward = {{8,8,10},
				 				 {10,5,1},
				 				 {10,5,10}};
		if(typeOfWorld == Constants.TRAINING)
			return trainingReward[sessionNum-1][row];
		else if(typeOfWorld == Constants.TESTING)
			return testingReward[sessionNum-1][row];
		else
			return 0;*/
		int[] reward = {1,5,10};
		return reward[row];
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
			/*int[][][] trainingTransitions = {{{100,0,0}, {0,100,0}, {0,0,100}},
					  					   {{100,0,0}, {50,50,0}, {0,50,50}},
					  					   {{100,0,0}, {0,100,0}, {0,0,100}}};
			
			int[][][] testingTransitions = {{{70,30,0}, {30,70,0}, {0,50,50}},
					   						{{33,33,33}, {0,50,50}, {0,0,100}},
					   						{{50,50,0}, {50,50,50}, {0,50,50}}};
			
			trainingTransitions = createProbArray(trainingTransitions);
			testingTransitions = createProbArray(testingTransitions);			
			
			int[][][] currTransitions = null;
			if(typeOfWorld == Constants.TRAINING){
				currTransitions = trainingTransitions;
			} else if(typeOfWorld == Constants.TESTING){
				currTransitions = testingTransitions;
			}
			for(int i=0; i<Constants.NUM_ROWS; i++){
				int randNum = Tools.rand.nextInt(100);
				if(randNum < currTransitions[sessionNum-1][0][i])
					queueItems[i] = 0;
				if(randNum < currTransitions[sessionNum-1][1][i])
					queueItems[i] = 1;
				if(randNum < currTransitions[sessionNum-1][2][i])
					queueItems[i] = 2;
			}*/
			
			/*System.out.print("Queue: ");
			for(int i=0; i<queueItems.length; i++){
				System.out.print(queueItems[i]+", ");
			}
			System.out.println();*/
			
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			
			if(humanAction == Action.PICK_UP){
				newState.humanItem = queueItems[newState.humanLoc.row];
			}
			if(robotAction == Action.PICK_UP){
				/*int item = queueItems[newState.robotLoc.row];
				int randNum = Tools.rand.nextInt(100);
				if(item == 2){
					if(randNum < 50)
						newState.robotItem = item;
				} else {
					newState.robotItem = item;
				}*/
				newState.robotItem = queueItems[newState.robotLoc.row];
			}
			if(humanAction == Action.DROP_OFF){
				currItemsLeft[newState.humanItem]--;
				newState.humanItem = -1;
			}
			if(robotAction == Action.DROP_OFF){
				currItemsLeft[newState.robotItem]--;
				newState.robotItem = -1;
			}
			
			newState.humanLoc = getNextStateLoc(newState.humanLoc, humanAction);
			newState.robotLoc = getNextStateLoc(newState.robotLoc, robotAction);
			
			int randNum0 = Tools.rand.nextInt(100);
			int randNum1 = Tools.rand.nextInt(100);
			int randNum2 = Tools.rand.nextInt(100);
			if(randNum0 < 100)
				queueItems[0] = 0;
							
			if(randNum1 < 50)
				queueItems[1] = 0;
			else
				queueItems[1] = 1;
			
			if(randNum2 < 50)
				queueItems[2] = 1;
			else
				queueItems[2] = 2;	
			
			obstacleRow++;
			if(obstacleRow >= Constants.NUM_ROWS)
				obstacleRow = 0;
			
			if(Main.currWithSimulatedHuman && Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
		} catch(Exception e){
			e.printStackTrace();
		}
		return newState;
	}
	
	public int[][][] createProbArray(int[][][] transitions){
		for(int i=0; i<transitions.length; i++){
			for(int j=0; j<transitions[i].length; j++){
				int sum = 0;
				for(int k=0; k<transitions[i][j].length; k++){
					sum = sum + transitions[i][j][k];
					transitions[i][j][k] = sum;
					if(k == transitions[i][j].length-1)
						transitions[i][j][k] = 100;
				}
			}
		}
		return transitions;
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