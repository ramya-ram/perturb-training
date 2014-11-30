package code;

/**
 * A communication object that contains the type of communication (suggestion, update, accept, or reject)
 * and the appropriate human action and robot actions (suggestion will be the only comm type that has a non-null action)
 */
public class CommResponse {
	public CommType commType;
	public Action humanAction;
	public Action robotAction;
	
	public CommResponse(CommType commType, Action humanAction, Action robotAction){
		this.commType = commType;
		this.humanAction = humanAction;
		this.robotAction = robotAction;
	}
	
	public String toString() {
		return commType+" - Human: "+humanAction+" Robot: "+robotAction;
	}
}
