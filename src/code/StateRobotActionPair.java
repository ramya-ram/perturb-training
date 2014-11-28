package code;

/**
 * Stores a state and a corresponding robot action
 */
public class StateRobotActionPair {
	private State state;
	private Action robotAction;

	public StateRobotActionPair(State state, Action robotAction) {
		this.state = state;
		this.robotAction = robotAction;
	}

	public State getState() {
		return state;
	}

	public Action getRobotAction() {
		return robotAction;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	public int hashCode() {
		return 8;
	}
	
	public boolean equals(Object Obj){
		StateRobotActionPair sap = (StateRobotActionPair)Obj;
		if(!state.equals(sap.state))
			return false;
		if(robotAction != sap.robotAction)
			return false;
		return true;
	}
	
	public String toString() {
		return "State: "+state+" Robot: "+robotAction;
	}
}