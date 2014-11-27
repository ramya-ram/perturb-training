package code;

public class StateHumanActionPair {
	private State state;
	private Action humanAction;

	public StateHumanActionPair(State state, Action humanAction) {
		this.state = state;
		this.humanAction = humanAction;
	}

	public State getState() {
		return state;
	}

	public Action getHumanAction() {
		return humanAction;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	public int hashCode() {
		return 6;
	}
	
	public boolean equals(Object Obj){
		StateHumanActionPair sap = (StateHumanActionPair)Obj;
		if(!state.equals(sap.state))
			return false;
		if(humanAction != sap.humanAction)
			return false;
		return true;
	}
	
	public String toString() {
		return "State: "+state+" Human: "+humanAction;
	}
}
