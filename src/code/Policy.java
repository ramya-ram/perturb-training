package code;

/**
 * Represents a policy for the human and robot
 * For every state in the task, this policy specifies what joint action to take 
 */
public class Policy {
	HumanRobotActionPair[] policy;
	
	public Policy(HumanRobotActionPair[] policy) {
		this.policy = policy;
	}
	
	public HumanRobotActionPair action(int stateId) {
		return policy[stateId];
	}
	
	public String toString() {
		String str="";
		for(int stateId=0; stateId<policy.length; stateId++){
			str+=stateId+" -> "+policy[stateId]+"\n";
		}
		return str;
	}
}