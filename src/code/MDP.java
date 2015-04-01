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
	public List<State> states;
	
	public MDP(List<State> states, HumanAgent humanAgent, RobotAgent robotAgent) {
		this.states = states;
		this.humanAgent = humanAgent;
		this.robotAgent = robotAgent;
	}

	public List<State> states() {
		return states;
	}

	public HumanAgent getHumanAgent() {
		return humanAgent;
	}

	public RobotAgent getRobotAgent() {
		return robotAgent;
	}
}
