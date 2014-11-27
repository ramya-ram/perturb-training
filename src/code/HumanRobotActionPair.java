package code;

/**
 * Represents a joint action
 */
public class HumanRobotActionPair {
	private Action humanAction;
	private Action robotAction;

	public HumanRobotActionPair(Action humanAction, Action robotAction) {
		this.humanAction = humanAction;
		this.robotAction = robotAction;
	}
	
	public Action getRobotAction() {
		return robotAction;
	}

	public Action getHumanAction() {
		return humanAction;
	}
	
	public int hashCode() {
		return 3;
	}
	
	public boolean equals(Object Obj){
		HumanRobotActionPair sap = (HumanRobotActionPair)Obj;
		if(humanAction != sap.humanAction)
			return false;
		if(robotAction != sap.robotAction)
			return false;
		return true;
	}
	
	public HumanRobotActionPair clone() {
		return new HumanRobotActionPair(humanAction, robotAction);
	}
	
	public String toString() {
		return "Human: "+humanAction.name()+" Robot: "+robotAction.name();
	}
}
