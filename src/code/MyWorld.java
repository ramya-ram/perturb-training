package code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyWorld {
	public static MDP mdp;
	public static List<State> states = new ArrayList<State>();
	public static State[] initStates;
	public static int[][] stochasticity;
	public String predefinedText;
	public String textToDisplay;
	
	public int[] seq;
	public int lastCompletedIndex = -1;
	
	public int sessionNum; //specifies which training or testing round it is
	public boolean perturb; //specifies if this world is for perturbation or procedural training
	public int typeOfWorld; //specifies if this world is for training or testing
	
	public MyWorld(int typeOfWorld, boolean perturb, int sessionNum, int[] seq){
		this.typeOfWorld = typeOfWorld;
		this.perturb = perturb;
		this.sessionNum = sessionNum;
		this.seq = seq;
		
		//initialize the mdp only once
		if(mdp == null)
			mdp = initializeMDP();
		
		if(stochasticity == null){
			stochasticity = new int[2][Constants.NUM_PARTS];
			for(int i=0; i<stochasticity.length; i++){ //HUMAN stochasticity
				if(i>=0 && i<=3)
					stochasticity[0][i] = 80;
				else if(i>=4 && i<=7)
					stochasticity[0][i] = 70;
				else if(i==8)
					stochasticity[0][i] = 100;	
			}
			for(int i=0; i<stochasticity.length; i++){ //ROBOT stochasticity
				if(i>=0 && i<=3)
					stochasticity[1][i] = 80;
				else if(i>=4 && i<=7)
					stochasticity[1][i] = 100;
				else if(i==8)
					stochasticity[1][i] = 50;
			}
		}
		
		//System.out.println("testWind="+testWind+" testDryness="+testDryness+" simulationWind="+simulationWind+" simulationDryness="+simulationDryness);
	}
	
	public static State getStateFromFile(String str){
		int[] statesOfFires = new int[Constants.NUM_PARTS];
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
		int statesPerPart = Constants.STATES_PER_PART;
		for(int i=0; i<statesPerPart; i++){
			for(int j=0; j<statesPerPart; j++){
				for(int k=0; k<statesPerPart; k++){
					for(int l=0; l<statesPerPart; l++){
						for(int m=0; m<statesPerPart; m++){
							for(int n=0; n<statesPerPart; n++){
								for(int o=0; o<statesPerPart; o++){
									for(int p=0; p<statesPerPart; p++){
										for(int q=0; q<statesPerPart; q++){
											int[] statesOfFire = {i,j,k,l,m,n,o,p,q};
											State state = new State(statesOfFire);
											states.add(state);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		initStates = new State[1];
		initStates[0] = new State(new int[]{0,0,0,0,0,0,0,0,0});
		System.out.println("init states size "+initStates.length);
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
				for(int i=0; i<s.stateOfParts.length; i++){
					if(s.stateOfParts[i] == Constants.NONE || s.stateOfParts[i] == Constants.PARTIAL)
						possibleActions.add(Action.valueOf(Action.class, "PUT_"+i));
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
				if(isGoalState(s)){
					possibleActions.add(Action.WAIT);
					return possibleActions;
				}
				for(int i=0; i<s.stateOfParts.length; i++){
					if(s.stateOfParts[i] == Constants.NONE || s.stateOfParts[i] == Constants.PARTIAL)
						possibleActions.add(Action.valueOf(Action.class, "PUT_"+i));
				}
				//possibleActions.add(Action.WAIT);
				return possibleActions;
			}	
		};
	}
	
	public static boolean isGoalState(State state){
		return state.allItemsInState(Constants.COMPLETE);
	}
	
	public State initialState(){
		//if(Main.currWithSimulatedHuman && typeOfWorld == Constants.TESTING){
		//	int[] stateOfParts = {};
		//	return new State(stateOfParts);
		//}
		return initStates[Tools.rand.nextInt(initStates.length)];	
	}
	
	/**
	 * Computes reward for being in this state
	 * Reward is given based on the intensities of fires, burnouts get high negative reward, even after the goal is reached 
	 */
	public double reward(State state, HumanRobotActionPair agentActions, State nextState){		
		double reward = -1;
		
		int humanIndex = Integer.parseInt(agentActions.getHumanAction().name().substring(Constants.indexOfPartInAction, Constants.indexOfPartInAction+1));
		int robotIndex = Integer.parseInt(agentActions.getRobotAction().name().substring(Constants.indexOfPartInAction, Constants.indexOfPartInAction+1));
		
		boolean humanComplete = nextState.stateOfParts[humanIndex] == Constants.COMPLETE;
		boolean robotComplete = nextState.stateOfParts[robotIndex] == Constants.COMPLETE;
		
		if(!humanComplete)
			humanIndex = -1;
		if(!robotComplete)
			robotIndex = -1;
		
		if(lastCompletedIndex >= Constants.NUM_PARTS-1)
			return 0;
		
		reward += rewardForAction(humanIndex, robotIndex);
		
		return reward;
	}
	
	public double rewardForAction(int humanIndex, int robotIndex){
		double reward = 0;
		if(humanIndex == seq[lastCompletedIndex+1] || robotIndex == seq[lastCompletedIndex+1]){
			reward+=3;
			lastCompletedIndex += 1;
		}
		if(lastCompletedIndex >= Constants.NUM_PARTS-1)
			return 0;
		if(humanIndex == seq[lastCompletedIndex+1] || robotIndex == seq[lastCompletedIndex+1]){
			reward+=3;
			lastCompletedIndex += 1;
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
						//int fire = str.charAt(1)-48;
						//textToDisplay += getBurnoutMessage(fire)+"\n";
						//nextState = getNextStateAfterBurnout(nextState, fire);
					} else if(str.charAt(0) == 'S'){
						//int spreadTo = str.charAt(2)-48;
						//textToDisplay += getSpreadMessage(str.charAt(1)-48, spreadTo)+"\n";
						//nextState = getNextStateAfterSpread(nextState, spreadTo);
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
			if(Main.currWithSimulatedHuman){
				if(Constants.usePredefinedTestCases && typeOfWorld == Constants.TESTING){
					newState = getPredefinedNextState(newState, agentActions);
					return newState;
				}
			} 
			
			Action humanAction = agentActions.getHumanAction();
			Action robotAction = agentActions.getRobotAction();
			int humanIndex = -1;
			int robotIndex = -1;
			if(humanAction != Action.WAIT)
				humanIndex = Integer.parseInt(humanAction.name().substring(Constants.indexOfPartInAction, Constants.indexOfPartInAction+1));
			if(robotAction != Action.WAIT)
				robotIndex = Integer.parseInt(robotAction.name().substring(Constants.indexOfPartInAction, Constants.indexOfPartInAction+1));
			
			newState = getStateAfterActions(newState, humanIndex, robotIndex);

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
	public State getStateAfterActions(State newState, int humanIndex, int robotIndex){
		if(humanIndex != -1 && humanIndex == robotIndex){
			newState.stateOfParts[humanIndex] = Constants.COMPLETE;
		} else {
			if(humanIndex >= 0){
				int randNum1 = Tools.rand.nextInt(100);
				if(randNum1 < stochasticity[0][humanIndex])
					newState.stateOfParts[humanIndex]+=2;
				else
					newState.stateOfParts[humanIndex]+=1;
				if(newState.stateOfParts[humanIndex] > Constants.COMPLETE)
					newState.stateOfParts[humanIndex] = Constants.COMPLETE;
			}

			if(robotIndex >= 0){
				int randNum2 = Tools.rand.nextInt(100);
				if(randNum2 < stochasticity[1][robotIndex])
					newState.stateOfParts[robotIndex]+=2;
				else
					newState.stateOfParts[robotIndex]+=1;
				if(newState.stateOfParts[robotIndex] > Constants.COMPLETE)
					newState.stateOfParts[robotIndex] = Constants.COMPLETE;
			}
		}
		return newState;
	}
}