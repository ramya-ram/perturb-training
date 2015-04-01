package code;

public class Policy {
	HumanRobotActionPair[] policy;
	public Policy(HumanRobotActionPair[] policy) {
		this.policy = policy;
	}
	public HumanRobotActionPair action(int stateId){
		return policy[stateId];
	}	
	public String toString(){
		String str="";
		for(int stateId=0; stateId<policy.length; stateId++){
			str+=stateId+" -> "+policy[stateId]+"\n";
		}
		return str;
	}
}