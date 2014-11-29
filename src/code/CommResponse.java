package code;

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
