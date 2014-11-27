package code;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Defines the components of an MDP
 */
public class MDP {
	public HumanAgent humanAgent;
	public RobotAgent robotAgent;
	public Set<State> states;
	
	public MDP(Set<State> states, HumanAgent humanAgent, RobotAgent robotAgent) {
		this.states = states;
		this.humanAgent = humanAgent;
		this.robotAgent = robotAgent;
	}

	public Set<State> states() {
		return states;
	}

	public HumanAgent getHumanAgent() {
		return humanAgent;
	}

	public RobotAgent getRobotAgent() {
		return robotAgent;
	}
	
	public State[] statesAsArray() {
		return convertToStatesArray(states);
	}
	
	public State[] convertToStatesArray(Set<State> states) {
		List<State> statesList = new ArrayList<State>();
		statesList.addAll(states);
		State[] statesArr = new State[statesList.size()];
		for(int i=0; i<statesList.size(); i++)
			if(statesList.get(i) instanceof State)
				statesArr[i] = (State)statesList.get(i);
		return statesArr;
	}
	
	/**
	 * Returns an array where each element is a list of actions per agent
	 * So the array consists of all the possible joint actions [A1,...,An] for n agents
	 */
	public List<HumanRobotActionPair> getAllJointActions(State state) {
		Action[] humanActions = humanAgent.actions(state);
		Action[] robotActions = robotAgent.actions(state);
		
		List<HumanRobotActionPair> jointActions = new ArrayList<HumanRobotActionPair>();
		for(Action humanAction : humanActions){
			for(Action robotAction : robotActions){
				jointActions.add(new HumanRobotActionPair(humanAction, robotAction));
			}
		}
		return jointActions;
	}
}
