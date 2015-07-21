package code;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;

import code.Action;
import code.Constants;
import code.MyWorld;
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
	}
	
	/**
	 * This method can be used to change any variables in the task for each new episode
	 */
	public void reset(){
		return;
	}
	
	/**
	 * A string read from a file is converted to a state
	 */
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
							//for all possible values for each fire, create a new state and add it to the list of states
							int[] statesOfFire = {i,j,k,l,m};
							State state = new State(statesOfFire);
							states.add(state);
						}
					}
				}
			}
		}
		//calculate how many of the total states can be initial states (in this case, every fire can be in any state except burned out)
		int numInitStates = (int) Math.pow(statesPerFire - 1, Constants.NUM_FIRES);
		initStates = new State[numInitStates];
		int count=0;
		for(State state : states){
			//add all possible initial states to the array (during learning, one of these states will be randomly picked as the initial state for each episode)
			if(state.noItemsInState(Constants.BURNOUT)){
				initStates[count] = state.clone();
				count++;
			}
		}
		System.out.println("Init states size "+count);
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
					//a fire can be put out if it has intensity one, two, or three (cannot be put out if it's non-existent/none or burned out)
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
					//a fire can be put out if it has intensity one, two, or three (cannot be put out if it's non-existent/none or burned out)
					if(s.stateOfFires[i] > Constants.NONE && s.stateOfFires[i] < Constants.BURNOUT)
						possibleActions.add(Action.valueOf(Action.class, "PUT_OUT"+i));
				}
				return possibleActions;
			}	
		};
	}
	
	/**
	 * Determines if the given state is a goal state
	 */
	public boolean isGoalState(State state){
		return state.allItemsInState(Constants.NONE, Constants.BURNOUT);
	}
	
	/**
	 * Returns an initial state for the episode (either randomly from the initStates array or a specific initial state)
	 */
	public State initialState(){
		if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
			int[] stateOfFires = {1,1,0,3,3};
			return new State(stateOfFires);
		}
		return initStates[Constants.rand.nextInt(initStates.length)];	
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
	
	/**
	 * Computes the next state and prints appropriate messages on SocketTest based on saved predefined case from file
	 */
	public State getPredefinedNextState(State state, HumanRobotActionPair agentActions){
		try{
			textToDisplay = "";
			System.out.println("USING PREDEFINED");
			State nextState = getProcePredefinedNextState(state, agentActions).clone();
			textToDisplay += "State after your actions: "+nextState.toStringGUI()+"\n";
			if(Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
			
			if(sessionNum == 1 && sessionNum == 2){ //base task
				return nextState;  //assumes the 1st and 2nd test tasks have no wind/dryness
			} else {		
				String text = "";
				if(typeOfWorld == Constants.TESTING){
					if(sessionNum == 3){
						//the 3rd test task is a perturbed test case, with wind = 2, dryness = 9
						String str = Main.perturb1TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	
					} else if(sessionNum == 4){
						//the 4th test task is a perturbed test case, with wind = 9, dryness = 2
						String str = Main.perturb2TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	 
					} 
				}
				for(int i=0; i<text.length(); i+=3){
					if(text.length()<=1)
						break;
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
			textToDisplay += "Final state: "+nextState.toStringGUI();
			if(Main.gameView != null)
				Main.gameView.setAnnouncements(textToDisplay);
			return nextState;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get next state from the procedural predefined test case
	 */
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
			int wind = 0;
			int dryness = 0;
			
			if(Main.CURRENT_EXECUTION == Main.SIMULATION){
				//if running simulation executions, only use testWind and testDryness (the real values)
				wind = testWind;
				dryness = testDryness;
			} else {
				//when running human subject experiments, use testWind and testDryness (the real values in the environment) when working with the person
				//use simulationWind and simulationDryness (these are noisy observations of the real values) when the robot simulates in between working with the person
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
				textToDisplay += "State after your actions: "+beforeStochasticity.toStringGUI()+"\n";

			newState = getStateAfterWindDryness(newState, wind, dryness);
			
			if(!beforeStochasticity.equals(newState) && Main.currWithSimulatedHuman){
				textToDisplay += "Final state: "+newState.toStringGUI();
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
			int randNum = Constants.rand.nextInt(100);
			if(randNum < 90)
				newState.stateOfFires[humanFireIndex]-=3;
			else
				newState.stateOfFires[humanFireIndex]-=2;
			if(newState.stateOfFires[humanFireIndex] < 0)
				newState.stateOfFires[humanFireIndex] = Constants.NONE;
		} else {
			if(humanFireIndex >= 0){
				int randNum1 = Constants.rand.nextInt(100);
				if(randNum1 < 90)
					newState.stateOfFires[humanFireIndex]-=1;
				else
					newState.stateOfFires[humanFireIndex]-=2;
				if(newState.stateOfFires[humanFireIndex] < 0)
					newState.stateOfFires[humanFireIndex] = Constants.NONE;
			}

			if(robotFireIndex >= 0){
				int randNum2 = Constants.rand.nextInt(100);
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
			//the higher the dryness in the environment, the more likely fires will burn down the building
			int highBurnoutPercent = dryness*10 + 10;
			for(int i=0; i<newState.stateOfFires.length; i++){
				//for each fire in the highest intensity, there is some probability it will reach the burnout stage
				if(newState.stateOfFires[i] == Constants.HIGHEST){
					int randNum = Constants.rand.nextInt(100);
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
			//the higher the wind in the environment, the more likely fires will spread to neighboring locations
			int numSpreaded = 0;
			int highSpreadPercent = wind*10 + 10;
			int mediumSpreadPercent = highSpreadPercent - 10;
			int lowSpreadPercent = mediumSpreadPercent - 10;
			
			for(int i=0; i<newState.stateOfFires.length; i++){
				int spreadPercent = 0;
				//higher intensity fires also have a higher chance of spreading than lower intensity fires
				if(newState.stateOfFires[i] == Constants.HIGHEST-2)
					spreadPercent = lowSpreadPercent;
				else if(newState.stateOfFires[i] == Constants.HIGHEST-1)
					spreadPercent = mediumSpreadPercent;
				else if(newState.stateOfFires[i] == Constants.HIGHEST)
					spreadPercent = highSpreadPercent;
				
				if(spreadPercent > 0){
					if(checkIfValidFireLoc(i-1, newState.stateOfFires)){ //if a fire exists to the left and is lower than the highest intensity, spread to this location (increase the fire intensity)
						int randNum = Constants.rand.nextInt(100);
						if(randNum < spreadPercent && numSpreaded <= 4){ //limit the number of fires that increase from wind to 5
							newState.stateOfFires[i-1]++;
							numSpreaded++;
							predefinedText+="S"+i+(i-1)+"";
							String text = getSpreadMessage(i, i-1);
							if(Main.currWithSimulatedHuman)
								textToDisplay += text+"\n";
						}
					}
						
					if(checkIfValidFireLoc(i+1, newState.stateOfFires)){ //if a fire exists to the right and is lower than the highest intensity, spread to this location (increase the fire intensity)
						int randNum = Constants.rand.nextInt(100);
						if(randNum < spreadPercent && numSpreaded <= 4){ //limit the number of fires that increase from wind to 5
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
	 * Sets simulationWind and simulationDryness which are used when the robot simulates
	 * testWind and testDryness are used when working with the person
	 */
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
	
	/**
	 * Message for a fire spreading to another location
	 */
	public String getSpreadMessage(int spreadFrom, int spreadTo){
		return "Fire "+convertToFireName(spreadFrom)+
				" spread to fire "+convertToFireName(spreadTo)+" because of wind!!";
	}
	
	/**
	 * Message for a fire reaching the burnout stage
	 */
	public String getBurnoutMessage(int fire){
		return "Fire "+convertToFireName(fire)+" burned down the building because of dryness!!";
	}
	
	/**
	 * Check if the given index is a valid index in the array and if the fire is less than the highest intensity (so it can be increased)
	 */
	public boolean checkIfValidFireLoc(int index, int[] stateOfFires) {
		return index >= 0 && index < Constants.NUM_FIRES && stateOfFires[index] < Constants.HIGHEST;
	}
	
	/**
	 * Convert a string to its respective action
	 */
    public static Action getActionFromInput(String str){
    	str.toLowerCase();
    	Action action = null;
    	if(str.contains("alpha"))
			action = Action.PUT_OUT0;
		else if(str.contains("bravo"))
			action = Action.PUT_OUT1;
		else if(str.contains("charlie"))
			action = Action.PUT_OUT2;
		else if(str.contains("delta"))
			action = Action.PUT_OUT3;
		else if(str.contains("echo"))
			action = Action.PUT_OUT4;
    	return action;
    }
    
    /**
     * Updates the images of the 5 fires after each time step to reflect each fire's intensity level
     */
    public static void updateState(State state) {
    	Main.gameView.stateView.removeAll();
        for(int i=0; i < state.stateOfFires.length; i++){
        	JLabel label = new JLabel();
        	label.setIcon(Main.gameView.intensityImages[state.stateOfFires[i]]);    
        	Main.gameView.stateView.add(label);
        }
        Main.gameView.stateView.revalidate();
        Main.gameView.stateView.setBackground(Color.WHITE);
    }
	
	/**
	 * Adds wait time to simulate a human playing
	 */
	public void simulateWaitTime(State state) {
		int stateScore = 0;
		for(int i=0; i<state.stateOfFires.length; i++){
			int num = state.stateOfFires[i];
			if(num == Constants.BURNOUT)
				stateScore += 0;
			else
				stateScore += num;
		}
		System.out.println("score "+stateScore);
		try{
			if(stateScore < 10){
				int shortRandomTime = 6;
				System.out.println(shortRandomTime*1000);
				Thread.sleep(shortRandomTime*1000);
			} else {
				int longRandomTime = 8;
				System.out.println(longRandomTime*1000);
				Thread.sleep(longRandomTime*1000);
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Set label in the GUI for human subject experiments so participants which session and round they are in
	 */
	public void setTitleLabel(int roundNum, Color[] colorArray, int indexOfColor){
		String str = "";
		if(typeOfWorld == Constants.TRAINING)
			str+= "Training Session ";
		else if(typeOfWorld == Constants.TESTING){
			if(sessionNum == 1)
				str+= "Practice Testing Session ";
			else
				str+= "Testing Session ";
		} else
			str+= "Practice Session ";
		str += sessionNum+" -- Observation: Wind = "+simulationWind+" Dryness = "+simulationDryness;
		if(Main.gameView != null){
			if(colorArray != null && indexOfColor >= 0 && indexOfColor < colorArray.length)
				Main.gameView.setTitleAndRoundLabel(str, roundNum, colorArray[indexOfColor]);
			else
				Main.gameView.setTitleAndRoundLabel(str, roundNum, Color.BLACK);
		}
	}
	
	/**
	 * Printable version of actions, displayed in the GUI for human subject experiments
	 */
	public String getPrintableFromAction(Action action){
		if(action != Action.WAIT){
			int fireIndex = Integer.parseInt(action.name().substring(7, 8));
			return "extinguish "+MyWorld.convertToFireName(fireIndex);
		}
		return "have to wait this turn";
	}
	
	/**
	 * To be consistent across all participants, the initial state for human subject experiments was identical, specified here
	 */
	public State initialState(int roundNum){
		if(typeOfWorld == Constants.PRACTICE){
			if(roundNum == 1){
				int[] stateOfFires = {3,3,3,3,3};
				return new State(stateOfFires);
			} else if(roundNum == 2){
				int[] stateOfFires = {3,2,0,3,1};
				return new State(stateOfFires);
			}	
		} else if(typeOfWorld == Constants.TRAINING){
			if(perturb){
				switch(roundNum){
					case 1:
						int[] stateOfFires = {2,3,3,1,2};
						return new State(stateOfFires);
					case 2:
						int[] stateOfFires1 = {2,2,1,3,3};
						return new State(stateOfFires1);
					case 3:
						int[] stateOfFires2 = {0,2,1,2,3};
						return new State(stateOfFires2);
					case 4:
						int[] stateOfFires3 = {0,1,1,2,3};
						return new State(stateOfFires3);
					case 5:
						int[] stateOfFires4 = {3,3,3,2,3};
						return new State(stateOfFires4);
					case 6:
						int[] stateOfFires5 = {3,3,2,3,1};
						return new State(stateOfFires5);
				}
			} else if(!perturb) {
				switch(roundNum){
					case 1:
						int[] stateOfFires = {2,3,3,1,2};
						return new State(stateOfFires);
					case 2:
						int[] stateOfFires1 = {2,2,1,3,3};
						return new State(stateOfFires1);
					case 3:
						int[] stateOfFires2 = {3,1,3,1,2};
						return new State(stateOfFires2);
					case 4:
						int[] stateOfFires3 = {2,3,1,1,3};
						return new State(stateOfFires3);
					case 5:
						int[] stateOfFires4 = {3,3,2,3,3};
						return new State(stateOfFires4);
					case 6:
						int[] stateOfFires5 = {3,2,3,3,3};
						return new State(stateOfFires5);
				}
			}
		} else if(typeOfWorld == Constants.TESTING) {
			if(roundNum == 1){
				int[] stateOfFires = {0,1,1,1,0};
				return new State(stateOfFires);
			} else if(roundNum == 2){
				int[] stateOfFires = {3,1,3,1,1};
				return new State(stateOfFires);
			} else if(roundNum == 3){
				int[] stateOfFires = {1,0,3,3,1};
				return new State(stateOfFires);
			} else if(roundNum == 4){
				int[] stateOfFires = {0,1,1,1,3};
				return new State(stateOfFires);
			} 
		}
		return null;
	}
}