package code;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ExperimentData {
	public static double[][] humanStateActionCounts;
	public static double[][] robotStateActionCounts;
	
	public static void getExperimentData(File directory){
		int statesSize = MyWorld.states.size();
		int actionsSize = Action.values().length;
		humanStateActionCounts = new double[statesSize][actionsSize];
		robotStateActionCounts = new double[statesSize][actionsSize];
		try{
			for(File subject : directory.listFiles()){
				File episode = new File(subject.getAbsolutePath()+"/episode.txt");
				if(!episode.exists())
					continue;
				BufferedReader reader = new BufferedReader(new FileReader(episode));
				String line;
				while ((line = reader.readLine()) != null) {
					if(!line.isEmpty()){
						String[] strs = line.split(", ");
						//for(String s: strs)
						//	System.out.println(s);
						int stateId = MyWorld.getStateId(strs[0]);
						//System.out.println(subject.getName()+" "+MyWorld.mdp.states.get(stateId)+" ("+stateId+") "+Action.valueOf(strs[1]).ordinal()+" "+Action.valueOf(strs[2]).ordinal());
						humanStateActionCounts[stateId][Action.valueOf(strs[1]).ordinal()] += 1;
						robotStateActionCounts[stateId][Action.valueOf(strs[2]).ordinal()] += 1;
					}
				}
				reader.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
		for(int i=0; i<humanStateActionCounts.length; i++){
			int humanSum = 0;
			int robotSum = 0;
			for(int j=0; j<humanStateActionCounts[i].length; j++){
				humanSum += humanStateActionCounts[i][j];
				robotSum += robotStateActionCounts[i][j];
			}
			//divide all elements by the sum
			for(int j=0; j<humanStateActionCounts[i].length; j++){
				humanStateActionCounts[i][j] = humanStateActionCounts[i][j]/humanSum;
				robotStateActionCounts[i][j] = robotStateActionCounts[i][j]/robotSum;
			}
			//add elements like: [a, a+b, a+b+c...] to make it easy to sample
			for(int j=1; j<humanStateActionCounts[i].length; j++){
				humanStateActionCounts[i][j] = humanStateActionCounts[i][j]+humanStateActionCounts[i][j-1];
				robotStateActionCounts[i][j] = robotStateActionCounts[i][j]+robotStateActionCounts[i][j-1];
			}
		}
		
//		for(int i=0; i<humanStateActionCounts.length; i++){
//			System.out.print(MyWorld.mdp.states.get(i)+": ");
//			for(int j=0; j<humanStateActionCounts[i].length; j++){
//				if(!Double.isNaN(robotStateActionCounts[i][j]))
//					System.out.print(robotStateActionCounts[i][j]+" ");
//				else
//					System.out.print("--- ");
//			}
//			System.out.println();
//		}
	}
	
//	public static Action getHumanAction(MDP mdp, State state){
//		int stateId = state.getId();
//		int numActions = humanStateActionCounts[stateId].length;
//		if(Double.isNaN(humanStateActionCounts[stateId][0])){
//			Action[] possibleHumanActions = mdp.humanAgent.actions(state); //from robot state because that's what robot sees
//	        return possibleHumanActions[Constants.rand.nextInt(possibleHumanActions.length)];
//		}
//		double num = Constants.rand.nextDouble();
//		int index = 0;
//		while(num > humanStateActionCounts[stateId][index] && num < numActions)
//			index++;
//		return Action.values()[index];
//	}
//	
//	public static HumanRobotActionPair getJointAction(MDP mdp, State state){
//		int stateId = state.getId();
//		//int numActions = humanStateActionCounts[stateId].length;
//		if(Double.isNaN(humanStateActionCounts[stateId][0])){
//			Action[] possibleRobotActions = mdp.robotAgent.actions(state);
//			Action[] possibleHumanActions = mdp.humanAgent.actions(state); //from robot state because that's what robot sees
//	        Action robotAction = possibleRobotActions[Constants.rand.nextInt(possibleRobotActions.length)];
//	        Action humanAction = possibleHumanActions[Constants.rand.nextInt(possibleHumanActions.length)];
//	        return new HumanRobotActionPair(humanAction, robotAction);
//		}
//		double num = Constants.rand.nextDouble();
//		int humanIndex = 0;
//		while(num > humanStateActionCounts[stateId][humanIndex])
//			humanIndex++;
//		
//		double num2 = Constants.rand.nextDouble();
//		int robotIndex = 0;
//		while(num2 > robotStateActionCounts[stateId][robotIndex])
//			robotIndex++;
//		return new HumanRobotActionPair(Action.values()[humanIndex], Action.values()[robotIndex]);
//	}
}
