package code;

import java.util.HashSet;
import java.util.Set;

public class MyWorld {
	public static MDP mdp;
	public static Set<State> states = new HashSet<State>();
	public static State[] initStates;
	
	public static int STATES_PER_FIRE = 5;
	public static int PERTURB1_TEST_NUM = 5;
	public static int PERTURB2_TEST_NUM = 6;
	public static int PROCE_TEST_NUM = 4;
	public static int NUM_FIRES = 5;
	public static int NONE = 0, HIGHEST = 3, BURNOUT = 4;
	public static int indexOfFireInAction = 7;
	public static int NUM_VARIABLES = 2;
	
	public int testWind = 0;
	public int testDryness = 0;
	public int simulationWind = 0;
	public int simulationDryness = 0;
	public int trainingSessionNum;
	public boolean perturb;
	
	public MyWorld(boolean perturb, int trainingSessionNum){
		this.perturb = perturb;
		this.trainingSessionNum = trainingSessionNum;
		if(mdp == null)
			mdp = initializeMDP();
		System.out.println("Initializing MDP "+trainingSessionNum);
		setWindAndDryness();
	}
	
	public void setWindAndDryness(){		
		if(trainingSessionNum == PROCE_TEST_NUM){
			testWind = 3;
			testDryness = 9;
		} else if(trainingSessionNum == PERTURB1_TEST_NUM){
			testWind = 9;
			testDryness = 3;
		} else if(trainingSessionNum == PERTURB2_TEST_NUM){
			testWind = 4;
			testDryness = 6;
		} 
		else if(trainingSessionNum == 2){
			if(perturb){
				//testWind = 8;
				//testDryness = 1;
				
				simulationWind = 5;
				simulationDryness = 0;
			}
		} else if(trainingSessionNum == 3){
			if(perturb){
				//testWind = 1;
				//testDryness = 8;
				
				simulationWind = 0;
				simulationDryness = 5;
			}
		}
		System.out.println("Wind "+testWind+" Dryness "+testDryness);
	}
	
	public static State getStateFromFile(String str){
		int[] statesOfFires = new int[NUM_FIRES];
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
		for(int i=0; i<STATES_PER_FIRE; i++){
			for(int j=0; j<STATES_PER_FIRE; j++){
				for(int k=0; k<STATES_PER_FIRE; k++){
					for(int l=0; l<STATES_PER_FIRE; l++){
						for(int m=0; m<STATES_PER_FIRE; m++){
							int[] statesOfFire = {i,j,k,l,m};
							State state = new State(statesOfFire);
							states.add(state);
						}
					}
				}
			}
		}
		int numStates = (int) Math.pow(STATES_PER_FIRE - 1, NUM_FIRES);
		initStates = new State[numStates];
		int count=0;
		for(State state : states){
			if(state.noItemsInState(BURNOUT)){
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
					if(s.stateOfFires[i] > NONE && s.stateOfFires[i] < BURNOUT)
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
					if(s.stateOfFires[i] > NONE && s.stateOfFires[i] < BURNOUT)
						possibleActions.add(Action.valueOf(Action.class, "PUT_OUT"+i));
				}
				return possibleActions;
			}	
		};
	}
	
	public static boolean isGoalState(State state){
		return state.allItemsInState(NONE, BURNOUT);
	}
	
	public State initialState(){
		/*if(trainingSessionNum == PROCE_TEST_NUM || trainingSessionNum == PERTURB1_TEST_NUM || trainingSessionNum == PERTURB2_TEST_NUM){
			int[] stateOfFires = {1,1,0,3,3};
			return new State(stateOfFires);
		}*/
		if(Main.currWithSimulatedHuman){
			System.out.println("USING TEST STATE");
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
			return -(100*nextState.getNumItemsInState(BURNOUT));
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
			State nextState = state.clone();
			System.out.println("USING PREDEFINED");
			nextState = getStateFromFile(Main.proceTestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()]);
			//Main.connect.sendMessage("-------------------------------------\nState after your actions: "+nextState.toStringSimple());
			//if(trainingSessionNum == PROCE_TEST_NUM){
			//	return nextState;
			//} else {
			
				String text = "";
				if(trainingSessionNum == PROCE_TEST_NUM){
					String str = Main.perturb0TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
					if(str != null)
						text += str;	
				} else if(trainingSessionNum == PERTURB1_TEST_NUM){
					String str = Main.perturb1TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
					if(str != null)
						text += str;	
				} else if(trainingSessionNum == PERTURB2_TEST_NUM){
					String str = Main.perturb2TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
					if(str != null)
						text += str;	 
				} 
				for(int i=0; i<text.length(); i+=3){
					System.out.println("i "+i+" i+3 "+(i+3));
					String str = text.substring(i, i+3);
					System.out.println("str "+str);
					if(str.charAt(0) == 'B'){
						int fire = str.charAt(1)-48;
						Main.connect.sendMessage(getBurnoutMessage(fire));
						nextState = getNextStateAfterBurnout(nextState, fire);
					} else if(str.charAt(0) == 'S'){
						int spreadTo = str.charAt(2)-48;
						Main.connect.sendMessage(getSpreadMessage(str.charAt(1)-48, spreadTo));
						nextState = getNextStateAfterSpread(nextState, spreadTo);
					}
				}
			//}
			Main.connect.sendMessage("-------------------------------------\nFinal state: "+nextState.toStringSimple());
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
		State newState = state.clone();
		if(isGoalState(newState))
			return newState;

		try{
			int wind = 0;
			int dryness = 0;
			if(Main.currWithSimulatedHuman){
				/*if(Main.predefined && (trainingSessionNum == PROCE_TEST_NUM || trainingSessionNum == PERTURB1_TEST_NUM || trainingSessionNum == PERTURB2_TEST_NUM)){
					newState = getPredefinedNextState(newState, agentActions);
					return newState;
				}*/
				wind = testWind;
				dryness = testDryness;
				//System.out.println("currSimulating with human wind "+wind+" dryness "+dryness);
			} else {
				wind = simulationWind;
				dryness = simulationDryness;
			}
			//System.out.println("wind "+wind+" dryness "+dryness);
			
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			int humanFireIndex = -1;
			int robotFireIndex = -1;
			if(humanAction != Action.WAIT)
				humanFireIndex = Integer.parseInt(humanAction.name().substring(7, 8));
			if(robotAction != Action.WAIT)
				robotFireIndex = Integer.parseInt(robotAction.name().substring(7, 8));
			
			//(trainingSessionNum == PROCE_TEST_NUM || trainingSessionNum == PERTURB1_TEST_NUM || trainingSessionNum == PERTURB2_TEST_NUM)
			//	System.out.println("test simulation wind "+wind+" dryness "+dryness);
			
			if(Main.currWithSimulatedHuman){
				//System.out.println("wind "+wind+" dryness "+dryness);
				if(humanFireIndex != -1 && humanFireIndex == robotFireIndex){
					int randNum = Tools.rand.nextInt(100);
					if(randNum < 90)
						newState.stateOfFires[humanFireIndex]-=3;
					else
						newState.stateOfFires[humanFireIndex]-=2;
					if(newState.stateOfFires[humanFireIndex] < 0)
						newState.stateOfFires[humanFireIndex] = NONE;
				} else {
					if(humanFireIndex >= 0){
						int randNum1 = Tools.rand.nextInt(100);
						if(randNum1 < 90)
							newState.stateOfFires[humanFireIndex]-=1;
						else
							newState.stateOfFires[humanFireIndex]-=2;
						if(newState.stateOfFires[humanFireIndex] < 0)
							newState.stateOfFires[humanFireIndex] = NONE;
					}
	
					if(robotFireIndex >= 0){
						int randNum2 = Tools.rand.nextInt(100);
						if(randNum2 < 90)
							newState.stateOfFires[robotFireIndex]-=1;
						else
							newState.stateOfFires[robotFireIndex]-=2;
						if(newState.stateOfFires[robotFireIndex] < 0)
							newState.stateOfFires[robotFireIndex] = NONE;
					}
				}
			} else {
				if(humanFireIndex != -1 && humanFireIndex == robotFireIndex){
					newState.stateOfFires[humanFireIndex]-=3;
					if(newState.stateOfFires[humanFireIndex] < 0)
						newState.stateOfFires[humanFireIndex] = NONE;
				} else {
					if(humanFireIndex >= 0){
						newState.stateOfFires[humanFireIndex]-=1;
						if(newState.stateOfFires[humanFireIndex] < 0)
							newState.stateOfFires[humanFireIndex] = NONE;
					}
					if(robotFireIndex >= 0){
						newState.stateOfFires[robotFireIndex]-=1;
						if(newState.stateOfFires[robotFireIndex] < 0)
							newState.stateOfFires[robotFireIndex] = NONE;
					}
				}
			}
			
			//State beforeStochasticity = newState.clone();
			
			//if(trainingSessionNum == PROCE_TEST_NUM)
			//	return newState;

			if(dryness > 0){
				int highBurnoutPercent = dryness*10 + 10;
				//if(dryness == 9)
				//	System.out.println("burnoutpercent "+highBurnoutPercent);
				//System.out.println("burnout percent "+highBurnoutPercent);
				for(int i=0; i<newState.stateOfFires.length; i++){
					if(newState.stateOfFires[i] == HIGHEST){
						int randNum = Tools.rand.nextInt(100);
						if(randNum < highBurnoutPercent){
							newState.stateOfFires[i] = BURNOUT;
							String text = getBurnoutMessage(i);
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
					if(newState.stateOfFires[i] == HIGHEST-2)
						burnoutPercent = lowBurnoutPercent;
					else if(newState.stateOfFires[i] == HIGHEST-1)
						burnoutPercent = mediumBurnoutPercent;
					else if(newState.stateOfFires[i] == HIGHEST)
						burnoutPercent = highBurnoutPercent;
					if(burnoutPercent > 0){
						if(checkIfValidFireLoc(i-1, newState.stateOfFires)){
							int randNum = Tools.rand.nextInt(100);
							if(randNum < burnoutPercent && numSpreaded <= 4){
								newState.stateOfFires[i-1]++;
								numSpreaded++;
								String text = getSpreadMessage(i, i-1);
							}
						}
							
						if(checkIfValidFireLoc(i+1, newState.stateOfFires)){
							int randNum = Tools.rand.nextInt(100);
							if(randNum < burnoutPercent && numSpreaded <= 4){
								newState.stateOfFires[i+1]++;
								numSpreaded++;
								String text = getSpreadMessage(i, i+1);
							}
						}
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return newState;
	}
	
	public State getNextStateAfterBurnout(State state, int fire){
		State newState = state.clone();
		newState.stateOfFires[fire] = BURNOUT;
		return newState;
	}
	
	public State getNextStateAfterSpread(State state, int spreadTo){
		State newState = state.clone();
		newState.stateOfFires[spreadTo]++;
		return newState;
	}
	
	public String getSpreadMessage(int spreadFrom, int spreadTo){
		return "-------------------------------------\nFIRE "+LearningAlgorithm.convertToLetter(spreadFrom)+
				" SPREAD TO FIRE "+LearningAlgorithm.convertToLetter(spreadTo)+" BECAUSE OF THE WIND!!";
	}
	
	public String getBurnoutMessage(int fire){
		return "-------------------------------------\nFIRE "+LearningAlgorithm.convertToLetter(fire)+" BURNED OUT BECAUSE OF THE DRY ENVIRONMENT!!";
	}
	
	public boolean checkIfValidFireLoc(int index, int[] stateOfFires) {
		return index >= 0 && index < NUM_FIRES && stateOfFires[index] < HIGHEST;
	}
}