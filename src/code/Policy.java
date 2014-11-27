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

	public HumanRobotActionPair action(int stateId){
		return policy[stateId];
	}
	
	public double checkPolicySimilarity(Policy otherPolicy){
		int totalStateActions = 0;
		int sameActionStates = 0;
		for(int stateId=0; stateId<policy.length; stateId++){
			System.out.println(stateId+" "+policy[stateId]+" "+otherPolicy.policy[stateId]);
			HumanRobotActionPair actionPair = action(stateId);
			HumanRobotActionPair actionPair2 = otherPolicy.action(stateId);
			if(actionPair.getRobotAction() == actionPair2.getRobotAction())
				sameActionStates++;
			if(actionPair.getHumanAction() == actionPair2.getHumanAction())
				sameActionStates++;
			totalStateActions+=2;
		}
		System.out.println("totalStates "+totalStateActions);
		return ((double) sameActionStates)/totalStateActions;
	}
	
	public String toString(){
		String str="";
		for(int stateId=0; stateId<policy.length; stateId++){
			str+=stateId+" -> "+policy[stateId]+"\n";
		}
		return str;
	}
}