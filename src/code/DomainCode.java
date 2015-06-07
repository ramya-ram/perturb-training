package code;

import java.util.ArrayList;
import java.util.List;

import code.Constants;
import code.MyWorld;


public class DomainCode {
	
	public static List<List<MyWorld>> initializeWorlds(){
		List<List<MyWorld>> allWorlds = new ArrayList<List<MyWorld>>();
		
		int[] trainWind = null;
		int[] trainDryness = null;
		int[] testWind = null;
		int[] testDryness = null;
		
		if(Main.CURRENT_EXECUTION == Main.SIMULATION){
			trainWind = Constants.testWind_training_simulation;
			trainDryness = Constants.testDryness_training_simulation;
			testWind = Constants.testWind_testing_simulation;
			testDryness = Constants.testDryness_testing_simulation;
		} else {
			trainWind = Constants.testWind_training;
			trainDryness = Constants.testDryness_training;
			testWind = Constants.testWind_testing;
			testDryness = Constants.testDryness_testing;
		}
		
		Constants.NUM_TRAINING_SESSIONS = trainWind.length;
		Constants.NUM_TESTING_SESSIONS = testWind.length;
		
		Main.PRQLTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		Main.AdaPTTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		
		//construct practiceWorlds
		List<MyWorld> practiceWorlds = new ArrayList<MyWorld>();
		for(int i=1; i<=2; i++){
			MyWorld practiceWorld = new MyWorld(Constants.PRACTICE, false, i, 0, 0);
			practiceWorlds.add(practiceWorld);
		}
		allWorlds.add(practiceWorlds);
		
		//construct training worlds for procedural and perturbation
		List<MyWorld> trainingWorldsProce = new ArrayList<MyWorld>();
		List<MyWorld> trainingWorldsPerturb = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TRAINING_SESSIONS; i++){
			MyWorld proceWorld = new MyWorld(Constants.TRAINING, false, i, trainWind[0], trainDryness[0]);
			trainingWorldsProce.add(proceWorld);
			MyWorld perturbWorld = new MyWorld(Constants.TRAINING, true, i, trainWind[i-1], trainDryness[i-1]);
			trainingWorldsPerturb.add(perturbWorld);
		}
		allWorlds.add(trainingWorldsProce);
		allWorlds.add(trainingWorldsPerturb);
		
		//construct testing worlds for both training
		List<MyWorld> testingWorlds = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TESTING_SESSIONS; i++){
			MyWorld testWorld = new MyWorld(Constants.TESTING, true, i, testWind[i-1], testDryness[i-1]);
			testingWorlds.add(testWorld);
		}
		allWorlds.add(testingWorlds);
		return allWorlds;
	}
	
	public static void changeTestWorlds(List<MyWorld> testingWorlds){
		return;
	}
	
	public static void initForExperiments(List<MyWorld> trainingWorldsProce, List<MyWorld> trainingWorldsPerturb, List<MyWorld> testingWorlds){
		//sets simulation wind and dryness
		for(MyWorld trainWorld : trainingWorldsProce)
			trainWorld.setSimulationWindDryness(Constants.simulationWind_training[0], Constants.simulationDryness_training[0]);
		for(MyWorld trainWorld : trainingWorldsPerturb)
			trainWorld.setSimulationWindDryness(Constants.simulationWind_training[trainWorld.sessionNum-1], Constants.simulationDryness_training[trainWorld.sessionNum-1]);
		for(MyWorld testWorld : testingWorlds)
			testWorld.setSimulationWindDryness(Constants.simulationWind_testing[testWorld.sessionNum-1], Constants.simulationDryness_testing[testWorld.sessionNum-1]);
	}
}
