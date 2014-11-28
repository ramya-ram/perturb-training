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
}
