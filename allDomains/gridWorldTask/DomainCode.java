package code;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import code.Constants;
import code.ExperimentCondition;
import code.Main;
import code.MyWorld;

/**
 * Code specific to a particular task
 * Includes initializing the training and testing worlds and initializing anything specific for human subject experiments
 */
public class DomainCode {
	public static Location[] testingGoalLocs;
	
	/**
	 * Initialize training and testing worlds (and practice worlds for human subject experiments)
	 */
	public static List<List<MyWorld>> initializeWorlds(){
		List<List<MyWorld>> allWorlds = new ArrayList<List<MyWorld>>();
		
		allWorlds.add(null); //no practice world for this task -- only running simulations
		allWorlds.add(null); //no training world procedural for this task -- not running procedural training in simulations
		
		//construct training worlds for procedural and perturbation
		List<MyWorld> trainingWorldsPerturb = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TRAINING_SESSIONS; i++){
			MyWorld perturbWorld = new MyWorld(Constants.TRAINING, true, i, Constants.trainingGoalLocs[i-1]);
			trainingWorldsPerturb.add(perturbWorld);
		}
		allWorlds.add(trainingWorldsPerturb);
		
		//construct testing worlds for both training
		List<MyWorld> testingWorlds = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TESTING_SESSIONS; i++){
			MyWorld testWorld = new MyWorld(Constants.TESTING, true, i, null);
			testingWorlds.add(testWorld);
		}
		allWorlds.add(testingWorlds);
			
		//rewardOverTime and rewardLimited's first dimension is the number of conditions
		//because we also include a comparison to PRQL with different priors, we add Constants.NUM_TRAINING_SESSIONS, corresponding to PRQL using each training task Q-values as its prior
		Main.rewardOverTime = new double[ExperimentCondition.values().length+Constants.NUM_TRAINING_SESSIONS][Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		Main.rewardLimitedTime = new double[ExperimentCondition.values().length+Constants.NUM_TRAINING_SESSIONS][Constants.NUM_TESTING_SESSIONS];
		Main.closestTrainingTask = new int[ExperimentCondition.values().length][Constants.NUM_TESTING_SESSIONS];
		
		try {
			testingGoalLocs = new Location[Constants.NUM_AVERAGING];
			BufferedReader reader = new BufferedReader(new FileReader(new File("inputFiles\\goalLocs.csv")));
			String line;
			int count = 0;
		    while ((line = reader.readLine()) != null) {
		        int row = Integer.parseInt(""+line.charAt(0));
		        int col = Integer.parseInt(""+line.charAt(2));
		        if(count < testingGoalLocs.length)
		        	testingGoalLocs[count] = new Location(row,col);
		        count++;
		    }
		    reader.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return allWorlds;
	}
	
	/**
	 * Changes the test worlds for each simulation run, if needed
	 */
	public static void changeTestWorlds(int runNum, List<MyWorld> testingWorlds){
		for(MyWorld testWorld : testingWorlds){
			testWorld.changeGoalLoc(runNum);
		}
	}
	
	/**
	 * Initialization for human subject experiments, if needed
	 */
	public static void initForExperiments(List<MyWorld> trainingWorldsProce, List<MyWorld> trainingWorldsPerturb, List<MyWorld> testingWorlds){
		return;
	}
}
