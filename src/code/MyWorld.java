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
	
	//prior probabilities for environment variables = wind, dryness
	public static double[][] probObsGivenWind;
	public static double[][] probObsGivenDryness;
	
	public int testWind = 0; 
	public int testDryness = 0;
	public int simulationWind = 0; //noisy version of test wind and dryness
	public int simulationDryness = 0;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum, int testWind, int testDryness){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		this.testWind = testWind;
		this.testDryness = testDryness;
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
		if(probObsGivenWind == null || probObsGivenDryness == null)
			initPriorProbabilities();
	}
	
	public static State getStateFromFile(String str){
		int[] statesOfFires = new int[Constants.NUM_FIRES];
		for(int i=0; i<str.length(); i++)
			statesOfFires[i] = str.charAt(i)-'0';
		return new State(statesOfFires);
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
		int statesPerFire = Constants.STATES_PER_FIRE;
		for(int i=0; i<statesPerFire; i++){
			for(int j=0; j<statesPerFire; j++){
				for(int k=0; k<statesPerFire; k++){
					for(int l=0; l<statesPerFire; l++){
						for(int m=0; m<statesPerFire; m++){
							int[] statesOfFire = {i,j,k,l,m};
							State state = new State(statesOfFire);
							states.add(state);
						}
					}
				}
			}
		}
		int numStates = (int) Math.pow(statesPerFire - 1, Constants.NUM_FIRES);
		initStates = new State[numStates];
		int count=0;
		for(State state : states){
			if(state.noItemsInState(Constants.BURNOUT)){
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
				if(isGoalState(s)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}
				for(int i=0; i<s.stateOfFires.length; i++){
					if(s.stateOfFires[i] > Constants.NONE && s.stateOfFires[i] < Constants.BURNOUT)
						possibleActions.add(Action.valueOf(Action.class, "PUT_OUT"+i));
				}
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
				if(isGoalState(s)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}
				for(int i=0; i<s.stateOfFires.length; i++){
					if(s.stateOfFires[i] > Constants.NONE && s.stateOfFires[i] < Constants.BURNOUT)
						possibleActions.add(Action.valueOf(Action.class, "PUT_OUT"+i));
				}
				return possibleActions;
			}	
		};
	}
	
	public static boolean isGoalState(State state){
		return state.allItemsInState(Constants.NONE, Constants.BURNOUT);
	}
	
	public State initialState(){
		if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			int[] stateOfFires = {1,1,0,3,3};
			return new State(stateOfFires);
		}
		return initStates[Tools.rand.nextInt(initStates.length)];	
	}
	
	/**
	 * Computes reward for being in this state
	 * Reward is given based on the intensities of fires, burnouts get high negative reward, even after the goal is reached 
	 */
	public double reward(State state, HumanRobotActionPair agentActions, State nextState){		
		if(isGoalState(nextState))
			return -(100*nextState.getNumItemsInState(Constants.BURNOUT));
		double reward = -10;
		for(int i=0; i<nextState.stateOfFires.length; i++){
			reward += -1*nextState.stateOfFires[i];
		}
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
			textToDisplay += "State after your actions: "+nextState.toStringSimple()+"\n";
			if(Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
			
			if(sessionNum == 1 && sessionNum == 2){ //base task
				return nextState;  //TODO: change if 1st and 2nd test task are not base tasks (no wind/dryness)
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
						nextState = getNextStateAfterBurnout(nextState, fire);
					} else if(str.charAt(0) == 'S'){
						int spreadTo = str.charAt(2)-48;
						textToDisplay += getSpreadMessage(str.charAt(1)-48, spreadTo)+"\n";
						nextState = getNextStateAfterSpread(nextState, spreadTo);
					}
				}
			}
			textToDisplay += "Final state: "+nextState.toStringSimple();
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
			int wind = 0;
			int dryness = 0;
			
			if(Main.CURRENT_EXECUTION == Main.SIMULATION){
				wind = testWind;
				dryness = testDryness;
			} else {
				if(Main.currWithSimulatedHuman){
					if(Constants.usePredefinedTestCases && typeOfWorld == Constants.TESTING){
						newState = getPredefinedNextState(newState, agentActions);
						return newState;
					}
					wind = testWind;
					dryness = testDryness;
				} else {
					wind = simulationWind;
					dryness = simulationDryness;
				}
			}
				
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			int humanFireIndex = -1;
			int robotFireIndex = -1;
			if(humanAction != Action.WAIT)
				humanFireIndex = Integer.parseInt(humanAction.name().substring(7, 8));
			if(robotAction != Action.WAIT)
				robotFireIndex = Integer.parseInt(robotAction.name().substring(7, 8));
			
			//if computing pre-defined next state
			if(Main.SUB_EXECUTION == Main.CREATE_PREDEFINED){
				if(Main.proceTestCase != null){
					newState = getProcePredefinedNextState(newState, agentActions).clone();
					newState = getStateAfterWindDryness(newState, wind, dryness);
					return newState;
				} else {
					newState = getStochasStateAfterActions(newState, humanFireIndex, robotFireIndex);
					predefinedText += newState.toStringFile();
					return newState;
				}
			}
			
			if(Main.currWithSimulatedHuman){
				newState = getStochasStateAfterActions(newState, humanFireIndex, robotFireIndex);
			} else {
				newState = getStateAfterActions(newState, humanFireIndex, robotFireIndex);
			}
			
			State beforeStochasticity = newState.clone();
			if(Main.currWithSimulatedHuman)
				textToDisplay += "State after your actions: "+beforeStochasticity.toStringSimple()+"\n";

			newState = getStateAfterWindDryness(newState, wind, dryness);
			
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
	public State getStochasStateAfterActions(State newState, int humanFireIndex, int robotFireIndex){
		if(humanFireIndex != -1 && humanFireIndex == robotFireIndex){
			int randNum = Tools.rand.nextInt(100);
			if(randNum < 90)
				newState.stateOfFires[humanFireIndex]-=3;
			else
				newState.stateOfFires[humanFireIndex]-=2;
			if(newState.stateOfFires[humanFireIndex] < 0)
				newState.stateOfFires[humanFireIndex] = Constants.NONE;
		} else {
			if(humanFireIndex >= 0){
				int randNum1 = Tools.rand.nextInt(100);
				if(randNum1 < 90)
					newState.stateOfFires[humanFireIndex]-=1;
				else
					newState.stateOfFires[humanFireIndex]-=2;
				if(newState.stateOfFires[humanFireIndex] < 0)
					newState.stateOfFires[humanFireIndex] = Constants.NONE;
			}

			if(robotFireIndex >= 0){
				int randNum2 = Tools.rand.nextInt(100);
				if(randNum2 < 90)
					newState.stateOfFires[robotFireIndex]-=1;
				else
					newState.stateOfFires[robotFireIndex]-=2;
				if(newState.stateOfFires[robotFireIndex] < 0)
					newState.stateOfFires[robotFireIndex] = Constants.NONE;
			}
		}
		return newState;
	}
	
	/**
	 * Deterministic version of the getStochasStateAfterActions() method
	 * Used when the robot simulates on its own, as this is the approximate model of the environment
	 * When working with the human in real live interactions, noise is added, as in getStochasStateAfterActions(), to model a real environment
	 */
	public State getStateAfterActions(State newState, int humanFireIndex, int robotFireIndex){
		if(humanFireIndex != -1 && humanFireIndex == robotFireIndex){
			newState.stateOfFires[humanFireIndex]-=3;
			if(newState.stateOfFires[humanFireIndex] < 0)
				newState.stateOfFires[humanFireIndex] = Constants.NONE;
		} else {
			if(humanFireIndex >= 0){
				newState.stateOfFires[humanFireIndex]-=1;
				if(newState.stateOfFires[humanFireIndex] < 0)
					newState.stateOfFires[humanFireIndex] = Constants.NONE;
			}
			if(robotFireIndex >= 0){
				newState.stateOfFires[robotFireIndex]-=1;
				if(newState.stateOfFires[robotFireIndex] < 0)
					newState.stateOfFires[robotFireIndex] = Constants.NONE;
			}
		}
		return newState;
	}
	
	/**
	 * More stochasticity added based on wind and dryness in the environment
	 * Wind causes fires to spread and dryness causes the building to burn down more quickly
	 */
	public State getStateAfterWindDryness(State newState, int wind, int dryness){
		if(dryness > 0){
			int highBurnoutPercent = dryness*10 + 10;
			for(int i=0; i<newState.stateOfFires.length; i++){
				if(newState.stateOfFires[i] == Constants.HIGHEST){
					int randNum = Tools.rand.nextInt(100);
					if(randNum < highBurnoutPercent){
						newState.stateOfFires[i] = Constants.BURNOUT;
						predefinedText+="B"+i+"#";
						String text = getBurnoutMessage(i);
						if(Main.currWithSimulatedHuman)
							textToDisplay += text+"\n";
					}
				}
			}
		}
		
		if(wind > 0){
			int numSpreaded = 0;
			int highBurnoutPercent = wind*10 + 10;
			int mediumBurnoutPercent = highBurnoutPercent - 10;
			int lowBurnoutPercent = mediumBurnoutPercent - 10;
			
			for(int i=0; i<newState.stateOfFires.length; i++){
				int burnoutPercent = 0;
				if(newState.stateOfFires[i] == Constants.HIGHEST-2)
					burnoutPercent = lowBurnoutPercent;
				else if(newState.stateOfFires[i] == Constants.HIGHEST-1)
					burnoutPercent = mediumBurnoutPercent;
				else if(newState.stateOfFires[i] == Constants.HIGHEST)
					burnoutPercent = highBurnoutPercent;
				if(burnoutPercent > 0){
					if(checkIfValidFireLoc(i-1, newState.stateOfFires)){
						int randNum = Tools.rand.nextInt(100);
						if(randNum < burnoutPercent && numSpreaded <= 4){
							newState.stateOfFires[i-1]++;
							numSpreaded++;
							predefinedText+="S"+i+(i-1)+"";
							String text = getSpreadMessage(i, i-1);
							if(Main.currWithSimulatedHuman)
								textToDisplay += text+"\n";
						}
					}
						
					if(checkIfValidFireLoc(i+1, newState.stateOfFires)){
						int randNum = Tools.rand.nextInt(100);
						if(randNum < burnoutPercent && numSpreaded <= 4){
							newState.stateOfFires[i+1]++;
							numSpreaded++;
							predefinedText+="S"+i+(i+1)+"";
							String text = getSpreadMessage(i, i+1);
							if(Main.currWithSimulatedHuman)
								textToDisplay += text+"\n";
						}
					}
				}
			}
		}
		return newState;
	}
	
	/**
	 * Initialize the prior probabilities of wind and dryness occurring 
	 * and the conditional probabilities of the observation being correct given the real values.
	 */
	public void initPriorProbabilities(){
		probObsGivenWind = new double[10][10];
		probObsGivenDryness = new double[10][10];
		
		for(int i=0; i<probObsGivenWind.length; i++){
			for(int j=0; j<probObsGivenWind[i].length; j++){
				if(i==j)
					probObsGivenWind[i][j] = 0.6;
				else if(Math.abs(i-j) == 1)
					probObsGivenWind[i][j] = 0.2;
			}
		}
		for(int j=0; j<probObsGivenWind[0].length; j++){
			double sum = 0;
			for(int i=0; i<probObsGivenWind.length; i++){
				sum+=probObsGivenWind[i][j];
			}
			sum *= 100;
			sum = Math.round(sum);
			sum /= 100;
			for(int i=0; i<probObsGivenWind.length; i++){
				probObsGivenWind[i][j] /= sum;
			}
		}
	}
	
	public void setSimulationWindDryness(int simulationWind, int simulationDryness){
		this.simulationWind = simulationWind;
		this.simulationDryness = simulationDryness;
	}
	
	/**
	 * Makes appropriate changes to the state to reflect the fire burning down the building
	 */
	public State getNextStateAfterBurnout(State state, int fire){
		State newState = state.clone();
		newState.stateOfFires[fire] = Constants.BURNOUT;
		return newState;
	}
	
	/**
	 * Makes appropriate changes to the state to reflect the fire spreading to the 'spreadTo' fire
	 */
	public State getNextStateAfterSpread(State state, int spreadTo){
		State newState = state.clone();
		newState.stateOfFires[spreadTo]++;
		return newState;
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
		return index >= 0 && index < Constants.NUM_FIRES && stateOfFires[index] < Constants.HIGHEST;
	}
}