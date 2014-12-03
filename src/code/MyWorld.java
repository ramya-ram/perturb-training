package code;

import java.util.HashSet;
import java.util.Set;

public class MyWorld {
	public static MDP mdp;
	public static Set<State> states = new HashSet<State>();
	public static State[] initStates;
	
	public int testWind = 0;
	public int testDryness = 0;
	public int simulationWind = 0;
	public int simulationDryness = 0;
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
		System.out.println("Initializing MDP "+sessionNum);
		
		//set appropriate levels of wind and dryness
		setWindAndDryness();
	}
	
	public void setWindAndDryness(){
		//setting testWind and testDryness for the testing scenarios
		if(typeOfWorld == Constants.TESTING){
			if(sessionNum == 1){
				testWind = 3;
				testDryness = 9;
			} else if(sessionNum == 2){
				testWind = 9;
				testDryness = 3;
			} else if(sessionNum == 3){
				testWind = 4;
				testDryness = 6;
			} 
		} else if(typeOfWorld == Constants.TRAINING){
			if(sessionNum == 2){
				if(perturb){
					//testWind = 8;
					//testDryness = 1;
					
					simulationWind = 5;
					simulationDryness = 0;
				}
			} else if(sessionNum == 3){
				if(perturb){
					//testWind = 1;
					//testDryness = 8;
					
					simulationWind = 0;
					simulationDryness = 5;
				}
			}
		}
		System.out.println("Wind "+testWind+" Dryness "+testDryness);
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
			String textToDisplay = "";
			State nextState = state.clone();
			System.out.println("USING PREDEFINED");
			nextState = getStateFromFile(Main.proceTestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()]);
			textToDisplay += "State after your actions: "+nextState.toStringSimple()+"\n";
			
			if(sessionNum == 1){ //base task
				return nextState;
			} else {		
				String text = "";
				if(typeOfWorld == Constants.TESTING){
					if(sessionNum == 1){
						String str = Main.perturb0TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	
					} else if(sessionNum == 2){
						String str = Main.perturb1TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	
					} else if(sessionNum == 3){
						String str = Main.perturb2TestCase[state.getId()][agentActions.getHumanAction().ordinal()][agentActions.getRobotAction().ordinal()];
						if(str != null)
							text += str;	 
					} 
				}
				for(int i=0; i<text.length(); i+=3){
					//System.out.println("i "+i+" i+3 "+(i+3));
					String str = text.substring(i, i+3);
					//System.out.println("str "+str);
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
		State newState = state.clone();
		if(isGoalState(newState))
			return newState;

		try{
			int wind = 0;
			int dryness = 0;
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
			
			String textToDisplay = "";
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			int humanFireIndex = -1;
			int robotFireIndex = -1;
			if(humanAction != Action.WAIT)
				humanFireIndex = Integer.parseInt(humanAction.name().substring(7, 8));
			if(robotAction != Action.WAIT)
				robotFireIndex = Integer.parseInt(robotAction.name().substring(7, 8));
			
			//(sessionNum == PROCE_TEST_NUM || sessionNum == PERTURB1_TEST_NUM || sessionNum == PERTURB2_TEST_NUM)
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
			} else {
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
			}
			
			State beforeStochasticity = newState.clone();
			if(Main.currWithSimulatedHuman)
				textToDisplay += "State after your actions: "+beforeStochasticity.toStringSimple()+"\n";
			
			if(typeOfWorld == Constants.TESTING && sessionNum == 1)
				return newState;

			if(dryness > 0){
				int highBurnoutPercent = dryness*10 + 10;
				for(int i=0; i<newState.stateOfFires.length; i++){
					if(newState.stateOfFires[i] == Constants.HIGHEST){
						int randNum = Tools.rand.nextInt(100);
						if(randNum < highBurnoutPercent){
							newState.stateOfFires[i] = Constants.BURNOUT;
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
								String text = getSpreadMessage(i, i+1);
								if(Main.currWithSimulatedHuman)
									textToDisplay += text+"\n";
							}
						}
					}
				}
			}
			if(!beforeStochasticity.equals(newState) && Main.currWithSimulatedHuman){
				textToDisplay += "Final state: "+newState.toStringSimple();
			}
			if(Main.currWithSimulatedHuman)
				Main.gameView.setAnnouncements(textToDisplay);
		} catch(Exception e){
			e.printStackTrace();
		}
		return newState;
	}
	
	public State getNextStateAfterBurnout(State state, int fire){
		State newState = state.clone();
		newState.stateOfFires[fire] = Constants.BURNOUT;
		return newState;
	}
	
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