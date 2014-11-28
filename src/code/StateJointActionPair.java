package code;

/**
 * Stores a state and the corresponding human and robot actions
 */
public class StateJointActionPair {
	private State state;
	private HumanRobotActionPair agentActions;

	public StateJointActionPair(State state, HumanRobotActionPair agentActions){
		this.state = state;
		this.agentActions = agentActions;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	public int hashCode() {
		return 7;
	}
	
	public boolean equals(Object Obj){
		StateJointActionPair sap = (StateJointActionPair)Obj;
		if(!state.equals(sap.state))
			return false;
		if(!agentActions.equals(sap.agentActions))
			return false;
		return true;
	}
	
	public String toString() {
		return state+" "+agentActions;
	}
}