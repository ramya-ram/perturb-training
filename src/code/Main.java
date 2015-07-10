package code;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import PR2_robot.Arduino;
import PR2_robot.GameView;
import PR2_robot.MyServer;

public class Main {
	public static int 
			SIMULATION = 0, //use for running simulation runs on the computer (compares AdaPT, PRQL with different priors, and Q-learning from scratch given limited simulation time)
			SIMULATION_HUMAN_TRAIN_TEST = 1, //use for human experiments where participants work with the simulation environment for training and testing
			SIMULATION_HUMAN_TRAIN = 2, //use for human experiments where participants work with the simulation environment only for training
			ROBOT_HUMAN_TEST = 3, //use for human experiments where participants work with the robot in testing after training in simulation
			CREATE_PREDEFINED = 4, //use for creating predefined test cases for human subject experiments (given a state and joint action, the next state will always be the same across participants)
			CREATE_OFFLINE_QVALUES = 5, //use for running offline deterministic simulations and having these values saved to a file so that the robot starts with base knowledge when working with a human
	        GENERATE_RBM_DATA = 6, //generate tuples from transition function to feed to a Restricted Boltzmann Machine (RBM)
	        REWARD_OVER_ITERS = 7; //evaluates reward received over the number of iterations over time (evaluates AdaPT and PRQL at specified intervals until some number of iterations)
	
	//choose one of the above options
	public static int INPUT = GENERATE_RBM_DATA;
	
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

	public static int CURRENT_EXECUTION = -1;
	public static int SUB_EXECUTION = -1;
	
	public static void main(String[] args){	
		if(INPUT == SIMULATION_HUMAN_TRAIN_TEST || INPUT == SIMULATION_HUMAN_TRAIN || INPUT == ROBOT_HUMAN_TEST){
			CURRENT_EXECUTION = INPUT;
			SUB_EXECUTION = -1;
		} else if(INPUT == SIMULATION){
			CURRENT_EXECUTION = SIMULATION;
			SUB_EXECUTION = -1;
		} else {
			CURRENT_EXECUTION = SIMULATION;
			SUB_EXECUTION = INPUT;
		}
		
		List<List<MyWorld>> allWorlds = DomainCode.initializeWorlds();
		List<MyWorld> practiceWorlds = allWorlds.get(0);
		List<MyWorld> trainingWorldsProce = allWorlds.get(1);
		List<MyWorld> trainingWorldsPerturb = allWorlds.get(2);
		List<MyWorld> testingWorlds = allWorlds.get(3);
		DomainCode.changeTestWorlds(testingWorlds);

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
			proceTestCase = readInPredefinedTestCase(testingWorlds.get(1), Constants.predefinedProceFileName);
			writePredefinedTestCase(testingWorlds.get(2), Constants.predefinedPerturb1FileName);
			writePredefinedTestCase(testingWorlds.get(3), Constants.predefinedPerturb2FileName);
			return;
		}
		
		try {
			if(Constants.useOfflineValues)
				populateOfflineQValues();
			if(Constants.usePredefinedTestCases){
				proceTestCase = readInPredefinedTestCase(testingWorlds.get(1), Constants.predefinedProceFileName);
				perturb1TestCase = readInPredefinedTestCase(testingWorlds.get(2), Constants.predefinedPerturb1FileName);
				perturb2TestCase = readInPredefinedTestCase(testingWorlds.get(3), Constants.predefinedPerturb2FileName);
			}
			saveToFile = true;
						
			if(CURRENT_EXECUTION == SIMULATION){
				if(SUB_EXECUTION == REWARD_OVER_ITERS){
					for(int i=0; i<Constants.NUM_AVERAGING; i++){
						System.out.println("*** "+i+" ***");
						DomainCode.changeTestWorlds(testingWorlds);
						//PERTURBATION - AdaPT
						TaskExecution AdaPT = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
						AdaPT.executeTask();
						
						//PERTURBATION - PRQL
						TaskExecution PRQL = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL);
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
						DomainCode.changeTestWorlds(testingWorlds);
															
						//PERTURBATION - AdaPT
						TaskExecution AdaPT = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
						AdaPT.executeTask();
						
						//PERTURBATION - PRQL
						TaskExecution PRQL = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL);
						PRQL.executeTask();
						
						//Standard QLearning
						TaskExecution QLearning = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.Q_LEARNING);
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
				DomainCode.initForExperiments(trainingWorldsProce, trainingWorldsPerturb, testingWorlds);
				
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
					TaskExecution proceQ = new TaskExecution(gameView, practiceWorlds, trainingWorldsProce, testingWorlds, ExperimentCondition.PROCE_Q);
					proceQ.executeTask();
				} else if(trainingType.equalsIgnoreCase("BQ")){
					//PERTURBATION - Q-learning
					TaskExecution perturbQ = new TaskExecution(gameView, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PERTURB_Q);
					perturbQ.executeTask();
				} else if(trainingType.equalsIgnoreCase("BH")){
					//PERTURBATION
					TaskExecution AdaPT = new TaskExecution(gameView, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
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
				if(myWorld.isGoalState(state))
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
	public static String[][][] readInPredefinedTestCase(MyWorld myWorld, String fileName){
		try{			
			String[][][] arr = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			String[] nextStates = reader.readLine().split(",");
			reader.close();
			System.out.println("next states size "+nextStates.length);
			
			int num=0;	
			for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);
				if(myWorld.isGoalState(state))
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
