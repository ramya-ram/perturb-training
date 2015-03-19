package code;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyWorld {
	public static MDP mdp;
	public static Set<State> states = new HashSet<State>();
	public static State[] initStates;
	public String predefinedText;
	public String textToDisplay;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public List<Integer> heavyItems = Arrays.asList(2,3,4);
	
	public List<Integer> humanPossibleItems = Arrays.asList(0,1,2,3);
	public List<Integer> robotPossibleItems = Arrays.asList(1,2,3,4);
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
		
		//System.out.println("testWind="+testWind+" testDryness="+testDryness+" simulationWind="+simulationWind+" simulationDryness="+simulationDryness);
	}
	
	public static State getStateFromFile(String str){
		int[] statesOfFires = new int[Constants.NUM_ITEMS];
		for(int i=0; i<str.length(); i++)
			statesOfFires[i] = str.charAt(i)-'0';
		return new State(statesOfFires, 0, 0);
	}
	
	/**
	 * Initializes the MDP for the particular task by specifying the set of states, the action function, the reward function, etc
	 */
	public MDP initializeMDP() {
		if(states == null || initStates == null)
			initStates();
		System.out.println("all states "+states.size());
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
		int statesPerItem = Constants.STATES_PER_ITEM;
		int numPos = Constants.NUM_POS;
		for(int i=0; i<statesPerItem; i++){
			for(int j=0; j<statesPerItem; j++){
				for(int k=0; k<statesPerItem; k++){
					for(int l=0; l<statesPerItem; l++){
						for(int m=0; m<statesPerItem; m++){
							int[] stateOfItems = {i,j,k,l,m};
							for(int humanPos=0; humanPos<numPos; humanPos++){
								for(int robotPos=0; robotPos<numPos; robotPos++){
									State state = new State(stateOfItems, humanPos, robotPos);	
									states.add(state);
								}
							}
						}
					}
				}
			}
		}
		int numStates = (int) Math.pow(statesPerItem, Constants.NUM_ITEMS)*Constants.NUM_POS*Constants.NUM_POS - Constants.NUM_POS*Constants.NUM_POS;
		System.out.println("num states "+numStates);
		initStates = new State[numStates];
		int count=0;
		for(State state : states){
			if(!state.allItemsInState(0)){
				initStates[count] = state.clone();
				count++;
			}
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
				//if(isGoalState(s)){
					//possibleActions.add(Action.WAIT);
					//return possibleActions;
				//}
				for(int i=0; i<s.stateOfItems.length; i++){
					if(s.stateOfItems[i] > 0 && robotPossibleItems.contains(i))
						possibleActions.add(Action.valueOf(Action.class, "GET_"+i));
				}
				if(possibleActions.isEmpty())
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
				//if(isGoalState(s)){
					//possibleActions.add(Action.WAIT);
				//	return possibleActions;
				//}
				for(int i=0; i<s.stateOfItems.length; i++){
					if(s.stateOfItems[i] > 0 && humanPossibleItems.contains(i))
						possibleActions.add(Action.valueOf(Action.class, "GET_"+i));
				}
				if(possibleActions.isEmpty())
					possibleActions.add(Action.WAIT);
				return possibleActions;
			}	
		};
	}
	
	public static boolean isGoalState(State state){
		return state.allItemsInState(0);
	}
	
	public State initialState(){
		if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			//int[] stateOfItems = {1,1,0,3,3};
			//return new State(stateOfItems, 0, 0);
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
		for(int i=0; i<nextState.stateOfItems.length; i++){
			reward += -1*nextState.stateOfItems[i];
		}*/
		//int humanNewIndex = agentActions.getHumanAction().ordinal();
		//int robotNewIndex = agentActions.getRobotAction().ordinal();
		
		double reward = -1;
		reward -= 1*Math.abs(nextState.humanPos - state.humanPos);
	    reward -= 1*Math.abs(nextState.robotPos - state.robotPos);
	    
		if(typeOfWorld == Constants.TRAINING && perturb && sessionNum == 3){
			if(heavyItems.contains(nextState.humanPos))
				reward -= 10;			
		}
	    
//		if(typeOfWorld == Constants.TRAINING && perturb && sessionNum == 3){
//			reward -= 50*nextState.stateOfItems[0];
//			reward -= 50*nextState.stateOfItems[Constants.NUM_ITEMS-1];
//		}
	
		return reward;
	}
	
	public State getProcePredefinedNextState(State state, HumanRobotActionPair agentActions) {
		return getStateFromFile(Main.proceTestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()]);
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
				//test
			} else {
				//simulation
			}
			
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			int humanItemIndex = -1;
			int robotItemIndex = -1;
			if(humanAction != Action.WAIT){
				humanItemIndex = Integer.parseInt(humanAction.name().substring(Constants.indexOfItemInAction));
				newState.humanPos = humanItemIndex;
			}
			if(robotAction != Action.WAIT){
				robotItemIndex = Integer.parseInt(robotAction.name().substring(Constants.indexOfItemInAction));
				newState.robotPos = robotItemIndex;
			}
			
			if(Main.currWithSimulatedHuman){
				newState = getStochasStateAfterActions(newState, humanItemIndex, robotItemIndex);
			} else {
				newState = getStateAfterActions(newState, humanItemIndex, robotItemIndex);
			}
			
			State beforeStochasticity = newState.clone();
			if(Main.currWithSimulatedHuman)
				textToDisplay += "State after your actions: "+beforeStochasticity.toStringSimple()+"\n";

			//newState = getStateAfterWindDryness(newState, wind, dryness);
			
			if(!beforeStochasticity.equals(newState) && Main.currWithSimulatedHuman){
				textToDisplay += "Final state: "+newState.toStringSimple();
			}
			if(Main.currWithSimulatedHuman && Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
		} catch(Exception e){
			e.printStackTrace();
		}
		return newState;
	}
	
	/**
	 * Adds in stochasticity into the environment
	 * For example, if the human and robot work separately on different fires, 90% of the time the fires go down by 1 level, 10% the fires go down by 2
	 * If the human and robot work together, 90% of the time the fire goes down by 3 levels, 10% the fire goes down by 2
	 * This noisy version of getStateAfterActions() models a real environment with stochasticity while the robot simulates with an approximate deterministic model as in getStateAfterActions()
	 */
	public State getStochasStateAfterActions(State newState, int humanItemIndex, int robotItemIndex){
		/*if(humanItemIndex != -1 && humanItemIndex == robotItemIndex){
			int randNum = Tools.rand.nextInt(100);
			if(randNum < 0)
				newState.stateOfItems[humanItemIndex]-=3;
			else
				newState.stateOfItems[humanItemIndex]-=1;
			if(newState.stateOfItems[humanItemIndex] < 0)
				newState.stateOfItems[humanItemIndex] = Constants.NONE;
		} else {*/
		int humanStochasPercent = 100;
		int robotStochasPercent = 100;
		
		if(typeOfWorld == Constants.TRAINING && perturb && sessionNum == 2){
			if(heavyItems.contains(robotItemIndex))
				robotStochasPercent = 50;
		}
			
		if(Main.currWithSimulatedHuman)
			System.out.println("humanStochasPercent "+humanStochasPercent+" robotStochasPercent "+robotStochasPercent);
		//if(typeOfWorld == Constants.TRAINING && perturb && sessionNum == 3)
			//robotStochasPercent = 10;
		
		//if(typeOfWorld == Constants.TESTING && perturb && sessionNum == 3)
			//robotStochasPercent = 30;
		
		if(humanItemIndex >= 0){
			int randNum1 = Tools.rand.nextInt(100);
			if(randNum1 < humanStochasPercent)
				newState.stateOfItems[humanItemIndex]-=1;
			if(newState.stateOfItems[humanItemIndex] < 0)
				newState.stateOfItems[humanItemIndex] = 0;
		}

		if(robotItemIndex >= 0){
			int randNum2 = Tools.rand.nextInt(100);
			if(randNum2 < robotStochasPercent)
				newState.stateOfItems[robotItemIndex]-=2;
			if(newState.stateOfItems[robotItemIndex] < 0)
				newState.stateOfItems[robotItemIndex] = 0;
		}
		//}
		return newState;
	}
	
	/**
	 * Deterministic version of the getStochasStateAfterActions() method
	 * Used when the robot simulates on its own, as this is the approximate model of the environment
	 * When working with the human in real live interactions, noise is added, as in getStochasStateAfterActions(), to model a real environment
	 */
	public State getStateAfterActions(State newState, int humanItemIndex, int robotItemIndex){
		/*if(humanItemIndex != -1 && humanItemIndex == robotItemIndex){
			newState.stateOfItems[humanItemIndex]-=3;
			if(newState.stateOfItems[humanItemIndex] < 0)
				newState.stateOfItems[humanItemIndex] = Constants.NONE;
		} else {*/
			if(humanItemIndex >= 0){
				newState.stateOfItems[humanItemIndex]-=1;
				if(newState.stateOfItems[humanItemIndex] < 0)
					newState.stateOfItems[humanItemIndex] = 0;
			}
			if(robotItemIndex >= 0){
				newState.stateOfItems[robotItemIndex]-=1;
				if(newState.stateOfItems[robotItemIndex] < 0)
					newState.stateOfItems[robotItemIndex] = 0;
			}
		//}
		return newState;
	}
}