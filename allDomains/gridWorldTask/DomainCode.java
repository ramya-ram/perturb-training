package code;


import java.util.ArrayList;
import java.util.List;

import code.Constants;
import code.MyWorld;

/**
 * Code specific to a particular task
 * Includes initializing the training and testing worlds and initializing anything specific for human subject experiments
 */
public class DomainCode {
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
		
		Main.PRQLTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		Main.AdaPTTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
				
		return allWorlds;
	}
	
	/**
	 * Changes the test worlds for each simulation run, if needed
	 */
	public static void changeTestWorlds(List<MyWorld> testingWorlds){
		for(MyWorld testWorld : testingWorlds){
			testWorld.changeGoalLoc();
		}
	}
	
	/**
	 * Initialization for human subject experiments, if needed
	 */
	public static void initForExperiments(List<MyWorld> trainingWorldsProce, List<MyWorld> trainingWorldsPerturb, List<MyWorld> testingWorlds){
		return;
	}
}
