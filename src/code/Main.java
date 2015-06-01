package code;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import PR2_robot.Arduino;
import PR2_robot.GameView;
import PR2_robot.MyServer;

public class Main {
	//USE AS VALUES FOR CURRENT_EXECUTION (Runs experiments), SUB_EXECUTION = NULL when using any of these
	public static int SIMULATION = 0, //use for running simulation runs on the computer
			SIMULATION_HUMAN_TRAIN_TEST = 1, //use for human experiments where participants work with the simulation environment for training and testing
			SIMULATION_HUMAN_TRAIN = 2, //use for human experiments where participants work with the simulation environment only for training
			ROBOT_HUMAN_TEST = 3; //use for human experiments where participants work with the robot in testing after training in simulation

	//USE AS VALUES FOR SUB_EXECUTION (Runs sub tasks) -- When using the sub executions, SET CURRENT_EXECUTION = SIMULATION;
	public static int CREATE_PREDEFINED = 4; //use for creating predefined test cases for human subject experiments (given a state and joint action, the next state will always be the same across participants)
	public static int CREATE_OFFLINE_QVALUES = 5; //use for running offline deterministic simulations and having these values saved to a file so that the robot starts with base knowledge when working with a human
	public static int GENERATE_RBM_DATA = 6; //generate tuples from transition function to feed to RBM
	public static int REWARD_OVER_ITERS = 7; //evaluates reward received over the number of iterations over time
	
	public static int CURRENT_EXECUTION = SIMULATION; //set CURRENT_EXECUTION to one of the above depending on which option you want to run
	public static int SUB_EXECUTION = REWARD_OVER_ITERS;
	
	public static boolean currWithSimulatedHuman = false;
	public static boolean saveToFile;
	
	//for robot experiments with humans
	public static GameView gameView;
	public static MyServer myServer;
	public static Arduino arduino;
	
	public static double[][][] jointQValuesOffline;
	public static double[][] robotQValuesOffline;
	public static String[][][] perturb2TestCase;
	public static String[][][] perturb1TestCase;
	public static String[][][] proceTestCase;
	
	public static double[][] PRQLTotal;
	public static double[][] AdaPTTotal;
	
	public static void main(String[] args){	
		PRQLTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		AdaPTTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		
		//construct training worlds for procedural and perturbation
		List<MyWorld> trainingWorldsProce = new ArrayList<MyWorld>();
		List<MyWorld> trainingWorldsPerturb = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TRAINING_SESSIONS; i++){
			MyWorld proceWorld = new MyWorld(Constants.TRAINING, false, i, Constants.testWind_train[0], Constants.testDryness_train[0]);
			trainingWorldsProce.add(proceWorld);
			MyWorld perturbWorld = new MyWorld(Constants.TRAINING, true, i, Constants.testWind_train[i-1], Constants.testDryness_train[i-1]);
			trainingWorldsPerturb.add(perturbWorld);
		}
		//construct testing worlds for both training
		List<MyWorld> testingWorlds = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TESTING_SESSIONS; i++){
			MyWorld testWorld = new MyWorld(Constants.TESTING, true, i, Constants.testWind_test[i-1], Constants.testDryness_test[i-1]);
			testingWorlds.add(testWorld);
		}
		
		if(SUB_EXECUTION == GENERATE_RBM_DATA){
			for(MyWorld trainWorld : trainingWorldsPerturb){
				QLearner learner = new QLearner(null, ExperimentCondition.ADAPT);
				learner.run(trainWorld, false /*withHuman*/);
				learner.run(trainWorld, true);
				learner.run(trainWorld, false /*withHuman*/);
				learner.run(trainWorld, true);
				learner.sampleTransitionFunc();
			}
			for(MyWorld testWorld : testingWorlds){
				QLearner learner = new QLearner(null, ExperimentCondition.ADAPT);
				learner.run(testWorld, false /*withHuman*/);
				learner.run(testWorld, true);
				learner.sampleTransitionFunc();
			}
			return;
		}
		
		if(SUB_EXECUTION == CREATE_OFFLINE_QVALUES){
			QLearner qLearnerProce = new QLearner(null, ExperimentCondition.PROCE_Q);
			qLearnerProce.run(trainingWorldsProce.get(0), false /*withHuman*/);
			qLearnerProce.saveOfflineLearning();
			return;
		}
		
		if(SUB_EXECUTION == CREATE_PREDEFINED){
			Main.currWithSimulatedHuman = true; //so that it uses test wind and test dryness
			//0th index is the practice testing session
			writePredefinedTestCase(testingWorlds.get(1), Constants.predefinedProceFileName);
			proceTestCase = readInPredefinedTestCase(Constants.predefinedProceFileName);
			writePredefinedTestCase(testingWorlds.get(2), Constants.predefinedPerturb1FileName);
			writePredefinedTestCase(testingWorlds.get(3), Constants.predefinedPerturb2FileName);
			return;
		}
		
		try {
			if(Constants.useOfflineValues)
				populateOfflineQValues();
			if(Constants.usePredefinedTestCases){
				proceTestCase = readInPredefinedTestCase(Constants.predefinedProceFileName);
				perturb1TestCase = readInPredefinedTestCase(Constants.predefinedPerturb1FileName);
				perturb2TestCase = readInPredefinedTestCase(Constants.predefinedPerturb2FileName);
			}
			saveToFile = true;
						
			if(CURRENT_EXECUTION == SIMULATION){
				if(SUB_EXECUTION == REWARD_OVER_ITERS){
					for(int i=0; i<Constants.NUM_AVERAGING; i++){
						System.out.println("*** "+i+" ***");
						//PERTURBATION - AdaPT
						TaskExecution AdaPT = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
						AdaPT.executeTask();
						
						//PERTURBATION - PRQL
						TaskExecution PRQL = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL);
						PRQL.executeTask();
					}
					BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.numIterName), true));
					
					for(int i=0; i<AdaPTTotal.length; i++){
						for(int j=0; j<AdaPTTotal[i].length; j++){
							rewardWriter.write((AdaPTTotal[i][j]/Constants.NUM_AVERAGING)+", ");
						}
						rewardWriter.write("\n");
					}
					rewardWriter.write("\n\n");
					
					for(int i=0; i<PRQLTotal.length; i++){
						for(int j=0; j<PRQLTotal[i].length; j++){
							rewardWriter.write((PRQLTotal[i][j]/Constants.NUM_AVERAGING)+", ");
						}
						rewardWriter.write("\n");
					}	
					rewardWriter.close();
					return;
				} else {
					for(int i=0; i<Constants.NUM_AVERAGING; i++){
						//makes simulation wind and dryness a noisy version of the real one
						System.out.println("*** "+i+" ***");
															
						//PERTURBATION - AdaPT
						TaskExecution AdaPT = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
						AdaPT.executeTask();
						
						//PERTURBATION - PRQL
						TaskExecution PRQL = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL);
						PRQL.executeTask();
						
						//Standard QLearning
						TaskExecution QLearning = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.Q_LEARNING);
						QLearning.executeTask();
						
						BufferedWriter rewardHRPerturbWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardAdaPTName), true));
						BufferedWriter rewardPRQLWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPRQLName), true));
						BufferedWriter rewardQLearningWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardQLearningName), true));
	
						rewardHRPerturbWriter.write("\n");
						rewardPRQLWriter.write("\n");
						rewardQLearningWriter.write("\n");
						
						rewardHRPerturbWriter.close();
						rewardPRQLWriter.close();
						rewardQLearningWriter.close();
					}
				}
			} else {	
				//sets simulation wind and dryness
				for(MyWorld trainWorld : trainingWorldsProce)
					trainWorld.setSimulationWindDryness(Constants.simulationWind_train[0], Constants.simulationDryness_train[0]);
				for(MyWorld trainWorld : trainingWorldsPerturb)
					trainWorld.setSimulationWindDryness(Constants.simulationWind_train[trainWorld.sessionNum-1], Constants.simulationDryness_train[trainWorld.sessionNum-1]);
				for(MyWorld testWorld : testingWorlds)
					testWorld.setSimulationWindDryness(Constants.simulationWind_test[testWorld.sessionNum-1], Constants.simulationDryness_test[testWorld.sessionNum-1]);
				
				gameView = new GameView(CURRENT_EXECUTION);
				if(CURRENT_EXECUTION == ROBOT_HUMAN_TEST){
					myServer = new MyServer();
					myServer.initConnections();
					arduino = new Arduino();
					arduino.initialize();
				}
				
				System.out.print("ParticipantID: ");
				String nameParticipant = Tools.scan.next();
				File dir = new File(Constants.participantDir+nameParticipant);
				dir.mkdir();
				Constants.participantDir = Constants.participantDir+nameParticipant+"\\";
				System.out.print("TrainingType (PQ or BH or BQ): "); 
				String trainingType = Tools.scan.next();

				if(trainingType.equalsIgnoreCase("PQ")){
					//PROCEDURAL - Q-learning
					TaskExecution proceQ = new TaskExecution(gameView, trainingWorldsProce, testingWorlds, ExperimentCondition.PROCE_Q);
					proceQ.executeTask();
				} else if(trainingType.equalsIgnoreCase("BQ")){
					//PERTURBATION - Q-learning
					TaskExecution perturbQ = new TaskExecution(gameView, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PERTURB_Q);
					perturbQ.executeTask();
				} else if(trainingType.equalsIgnoreCase("BH")){
					//PERTURBATION
					TaskExecution AdaPT = new TaskExecution(gameView, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
					AdaPT.executeTask();
				}
				Main.gameView.initTitleGUI("end");
				Main.gameView.setTitleAndRoundLabel("", 0, Color.BLACK);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Initialize q values from offline learning (saved in a file)
	 */
	public static void populateOfflineQValues(){
		try{
			jointQValuesOffline = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];//new HashMap<StateJointActionPair, Double>();
			BufferedReader jointReader = new BufferedReader(new FileReader(new File(Constants.jointQValuesFile)));
			String[] jointValues = jointReader.readLine().split(",");
			
			robotQValuesOffline = new double[MyWorld.mdp.states.size()][Action.values().length];
			BufferedReader robotReader = new BufferedReader(new FileReader(new File(Constants.robotQValuesFile)));
			String[] robotValues = robotReader.readLine().split(",");
			
			System.out.println("joint size "+jointValues.length);
			System.out.println("robot size "+robotValues.length);

			jointReader.close();
			robotReader.close();
							
			int jointNum=0;
			int robotNum=0;
			for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);												
				for(Action robotAction : Action.values()){
					double robotValue = Double.parseDouble(robotValues[robotNum]);
					if(robotValue != 0){
						robotQValuesOffline[state.getId()][robotAction.ordinal()] = robotValue;	
					}
					robotNum++;
					for(Action humanAction : Action.values()){
						double jointValue = Double.parseDouble(jointValues[jointNum]);
						if(jointValue != 0){
							jointQValuesOffline[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = jointValue;
						}
						jointNum++;
					}
				}
			}     
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Write the given test case to a file so that it can be loaded for each participant (this will ensure consistency among participants in the experiment)
	 */
	public static void writePredefinedTestCase(MyWorld myWorld, String fileName){
		try {
			int num = 0;
			System.out.println("in populate");
			File file = new File(fileName);
			if(file.exists())
				file.delete();
			BufferedWriter stateWriter = new BufferedWriter(new FileWriter(file, true));
			for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);
				if(MyWorld.isGoalState(state))
					continue;
				for(Action humanAction : Action.values()){
					for(Action robotAction : Action.values()){
						if((MyWorld.mdp.humanAgent.actionsAsList(state).contains(humanAction) || humanAction == Action.WAIT)
								&& (MyWorld.mdp.robotAgent.actionsAsList(state).contains(robotAction) || robotAction == Action.WAIT)){
							State nextState;
							HumanRobotActionPair agentActions;
							do{
								agentActions = new HumanRobotActionPair(humanAction, robotAction);
								nextState = myWorld.getNextState(state, agentActions);
								if(humanAction==Action.WAIT && robotAction==Action.WAIT)
									break;
							} while(state.equals(nextState));
							if(myWorld.predefinedText.length() > 0)
								stateWriter.write(myWorld.predefinedText+",");
							else
								stateWriter.write("-,");
							num++;
						}
					}
				}
			}
			System.out.println("size "+num);
			stateWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Read in from a file predefined test cases so that participants can be compared fairly on their performance in the testing sessions
	 */
	public static String[][][] readInPredefinedTestCase(String fileName){
		try{			
			String[][][] arr = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			String[] nextStates = reader.readLine().split(",");
			reader.close();
			System.out.println("next states size "+nextStates.length);
			
			int num=0;	
			for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);
				if(MyWorld.isGoalState(state))
					continue;
				for(Action humanAction : Action.values()){
					for(Action robotAction : Action.values()){
						if((MyWorld.mdp.humanAgent.actionsAsList(state).contains(humanAction) || humanAction == Action.WAIT)
								&& (MyWorld.mdp.robotAgent.actionsAsList(state).contains(robotAction) || robotAction == Action.WAIT)){
							
							String str1 = nextStates[num];
							if(str1.length() > 0)
								arr[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = str1;
							
							num++;
						}
					}
				}
			}
	        return arr;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
