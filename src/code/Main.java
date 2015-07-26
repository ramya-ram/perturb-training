package code;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import PR2_robot.Arduino;
import PR2_robot.GameView;
import PR2_robot.MyServer;

public class Main {
	public static int
			//HUMAN EXPERIMENTS
			SIMULATION_HUMAN_TRAIN_TEST = 1, //use for human experiments where participants work with the simulation environment for training and testing
			SIMULATION_HUMAN_TRAIN = 2, //use for human experiments where participants work with the simulation environment only for training
			ROBOT_HUMAN_TEST = 3, //use for human experiments where participants work with the robot in testing after training in simulation
			
			//CREATE VALUES/TASKS BEFORE RUNNING
			CREATE_PREDEFINED = 4, //use for creating predefined test cases for human subject experiments (given a state and joint action, the next state will always be the same across participants)
			CREATE_OFFLINE_QVALUES = 5, //use for running offline deterministic simulations and having these values saved to a file so that the robot starts with base knowledge when working with a human
	       	        
			//SIMULATION RUNS
	        REWARD_OVER_ITERS = 6, //evaluates reward received over the number of iterations over time (evaluates AdaPT, PRQL, Q-learning from scratch at specified intervals until some number of iterations)
	    	REWARD_LIMITED_TIME = 7; //compares AdaPT, PRQL with different priors, and Q-learning from scratch given limited simulation time
	
	//CHANGE WHEN RUNNING THIS PROGRAM: choose one of the above options and set it here
	public static int INPUT = REWARD_LIMITED_TIME;
	
	public static boolean currWithSimulatedHuman = false;
	public static boolean saveToFile;
	public static boolean writeRBMDataToFile = false;
	
	//for robot experiments with humans
	public static GameView gameView;
	public static MyServer myServer;
	public static Arduino arduino;
	
	//input data, including Q-values learned offline that gives robot base knowledge and predefined test cases for consistency in human subject experiments
	public static double[][][] jointQValuesOffline;
	public static double[][] robotQValuesOffline;
	public static String[][][] perturb2TestCase;
	public static String[][][] perturb1TestCase;
	public static String[][][] proceTestCase;
	
	//adds up reward over many simulation runs that then gets averaged to obtain an average performance of the algorithm over time
	//the first dimension is the condition, each has a 2D array in which the rows different test case and the columns represent reward over time
	public static double[][][] rewardOverTime;
	public static double[][] rewardLimitedTime;
	public static int[][] closestTrainingTask;
	
	public static int[][][] RBMTrainTaskData;
	public static int[][][] RBMTestTaskData;
	public static int currRBMDataNum = 0;

	//used by the program to determine what options to run (based on the INPUT variable that is set above but do NOT change this! this is automatically set)
	public static int CURRENT_EXECUTION = -1;
	public static int SUB_EXECUTION = -1;
	
	public static int SIMULATION = 0;
	
	public static MatlabProxyFactory factory;
	public static MatlabProxy proxy;
	
	public static void main(String[] args){	
		if(INPUT == SIMULATION_HUMAN_TRAIN_TEST || INPUT == SIMULATION_HUMAN_TRAIN || INPUT == ROBOT_HUMAN_TEST){
			CURRENT_EXECUTION = INPUT;
			SUB_EXECUTION = -1;
		} else {
			CURRENT_EXECUTION = SIMULATION;
			SUB_EXECUTION = INPUT;
		}
		
		//initializes practice, training, and testing worlds (domain-specific so they are initialized in DomainCode.java)
		List<List<MyWorld>> allWorlds = DomainCode.initializeWorlds();
		List<MyWorld> practiceWorlds = allWorlds.get(0);
		List<MyWorld> trainingWorldsProce = allWorlds.get(1);
		List<MyWorld> trainingWorldsPerturb = allWorlds.get(2);
		List<MyWorld> testingWorlds = allWorlds.get(3);
		DomainCode.changeTestWorlds(testingWorlds);
		
		int numFeatures = 2*(new State().toArrayRBM().length) + 2;
		RBMTrainTaskData = new int[Constants.NUM_TRAINING_SESSIONS][Constants.NUM_RBM_DATA_POINTS][numFeatures];
		RBMTestTaskData = new int[Constants.NUM_TESTING_SESSIONS][Constants.NUM_RBM_DATA_POINTS][numFeatures];
		
		//if option is create offline values, Q-learning will be run and the Q-values at the end of the learning will be saved to a file
		if(SUB_EXECUTION == CREATE_OFFLINE_QVALUES){
			QLearner qLearnerProce = new QLearner(null, ExperimentCondition.PROCE_Q);
			qLearnerProce.runQLearning(trainingWorldsProce.get(0), false /*withHuman*/);
			qLearnerProce.saveOfflineLearning();
			return;
		}
		
		//if option is create predefined test cases, a test case will be created for a procedural task and then two perturbation tasks
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
				populateOfflineQValues(); //read in offline values if using them
			if(Constants.usePredefinedTestCases){ //read in predefined test cases if using them
				proceTestCase = readInPredefinedTestCase(testingWorlds.get(1), Constants.predefinedProceFileName);
				perturb1TestCase = readInPredefinedTestCase(testingWorlds.get(2), Constants.predefinedPerturb1FileName);
				perturb2TestCase = readInPredefinedTestCase(testingWorlds.get(3), Constants.predefinedPerturb2FileName);
			}
			saveToFile = true;
			
			if(CURRENT_EXECUTION == SIMULATION){ //if running anything in simulation (multiple options are part of this)
				MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();
				
				//create a proxy to control MATLAB
			    factory = new MatlabProxyFactory(options);
			    proxy = factory.getProxy();
			    
			    //add matlab code path
			  	String addPath = "addpath('"+Paths.get("").toAbsolutePath().toString()+"\\RBM_MatlabCode')";
			  	Main.proxy.eval(addPath);
				
				if(SUB_EXECUTION == REWARD_OVER_ITERS){ //compares algorithms on how quickly they learn (can be used to plot a learning curve showing how the agent learns the task over time)
					BufferedWriter closestTrainingTaskWriter = new BufferedWriter(new FileWriter(new File(Constants.closestTrainingTask)));
					for(int num=0; num<closestTrainingTask.length; num++){
						closestTrainingTaskWriter.write(""+ExperimentCondition.values()[num]+",,");
						for(int j=0; j<closestTrainingTask[num].length; j++){
							closestTrainingTaskWriter.write(",");
						}
					}
					closestTrainingTaskWriter.write("\n");
					for(int i=0; i<Constants.NUM_AVERAGING; i++){
						System.out.println("*** "+i+" ***");
						runAllConditions(practiceWorlds, trainingWorldsPerturb, testingWorlds);
						for(int num=0; num<closestTrainingTask.length; num++){
							for(int j=0; j<closestTrainingTask[num].length; j++){
								closestTrainingTaskWriter.write(closestTrainingTask[num][j]+",");
							}
							closestTrainingTaskWriter.write(",,");
						}
						closestTrainingTaskWriter.write("\n");
					}
					closestTrainingTaskWriter.close();
					
					BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardOverIters)));
					
					//the first dimension of rewardOverTime is the condition (e.g. AdaPT, PRQL)
					//within the condition is a 2D array, the columns represent reward over time, rows represent different test tasks
					//when running multiple simulation runs in one test case/row, the reward over time is added up for that row so that when averaged, 
					//that row represents a robust learning over time curve for that test case
					
					for(int num=0; num<rewardOverTime.length; num++){
						if(num < ExperimentCondition.values().length)
							rewardWriter.write(""+ExperimentCondition.values()[num]+"\n");
						else
							rewardWriter.write("PRQL"+(num-ExperimentCondition.values().length)+"\n");
						for(int i=0; i<rewardOverTime[num].length; i++){
							for(int j=0; j<rewardOverTime[num][i].length; j++){
								rewardWriter.write((rewardOverTime[num][i][j]/Constants.NUM_AVERAGING)+", ");
							}
							rewardWriter.write("\n");
						}
						rewardWriter.write("\n\n");
					}
					rewardWriter.close();
				} else if(SUB_EXECUTION == REWARD_LIMITED_TIME){ //compares the algorithms after simulating for a limited number of iterations
					for(int i=0; i<Constants.NUM_AVERAGING; i++){
						System.out.println("*** "+i+" ***");
						runAllConditions(practiceWorlds, trainingWorldsPerturb, testingWorlds);
					}
					
					BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardLimitedTime)));
					//the first dimension of rewardOverTime is the condition (e.g. AdaPT, PRQL)
					//within the condition is a 2D array, the columns represent reward over time, rows represent different test tasks
					//when running multiple simulation runs in one test case/row, the reward over time is added up for that row so that when averaged, 
					//that row represents a robust learning over time curve for that test case
					
					for(int num=0; num<rewardOverTime.length; num++){
						if(num < ExperimentCondition.values().length)
							rewardWriter.write(""+ExperimentCondition.values()[num]+"\n");
						else
							rewardWriter.write("PRQL"+(num-ExperimentCondition.values().length)+"\n");
						for(int i=0; i<rewardLimitedTime[num].length; i++){
							rewardWriter.write((rewardLimitedTime[num][i]/Constants.NUM_AVERAGING)+", ");
						}
						rewardWriter.write("\n\n");
					}
					rewardWriter.close();		
				}
				
				//remove matlab code path
				String removePath = "rmpath('"+Paths.get("").toAbsolutePath().toString()+"\\RBM_MatlabCode')";
			  	proxy.eval(removePath);
			  	
			    //disconnect the proxy from MATLAB
			    proxy.disconnect();
	    
			} else { //for human subject experiments
				//initialize any domain-specific variables for experiments, if needed
				DomainCode.initForExperiments(trainingWorldsProce, trainingWorldsPerturb, testingWorlds);
				
				gameView = new GameView(CURRENT_EXECUTION);
				if(CURRENT_EXECUTION == ROBOT_HUMAN_TEST){
					myServer = new MyServer();
					myServer.initConnections();
					arduino = new Arduino();
					arduino.initialize();
				}
				
				//make a directory for each participant
				System.out.print("ParticipantID: ");
				String nameParticipant = Constants.scan.next();
				File dir = new File(Constants.participantDir+nameParticipant);
				dir.mkdir();
				Constants.participantDir = Constants.participantDir+nameParticipant+"\\";
				//participant either assigned to procedural Q-learning (PQ), perturbation Q-learning (BQ), perturbation AdaPT(BH)
				System.out.print("TrainingType (PQ or BQ or BH): "); 
				String trainingType = Constants.scan.next();

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
	 * Run AdaPT, PRQL, and standard Q-learning for all training and test tasks
	 */
	public static void runAllConditions(List<MyWorld> practiceWorlds, List<MyWorld> trainingWorldsPerturb, List<MyWorld> testingWorlds){
		DomainCode.changeTestWorlds(testingWorlds);
		PRQLearner.bestPriorReward = new double[Constants.NUM_TESTING_SESSIONS];
		for(int i=0; i<PRQLearner.bestPriorReward.length; i++)
			PRQLearner.bestPriorReward[i] = Integer.MIN_VALUE;
		
		//PERTURBATION - AdaPT
		TaskExecution AdaPT = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.ADAPT);
		AdaPT.executeTask();
		
		//PERTURBATION - PRQL
		TaskExecution PRQL = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL);
		PRQL.executeTask();
		
		//PERTURBATION - PRQL using RBM prior
		TaskExecution PRQL_RBM = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL_RBM);
		PRQL_RBM.executeTask();
		
		//Standard QLearning
		TaskExecution QLearning = new TaskExecution(null, practiceWorlds, trainingWorldsPerturb, testingWorlds, ExperimentCondition.Q_LEARNING);
		QLearning.executeTask();
	}
	
	/**
	 * Initialize Q-values from offline learning (saved in a file)
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
				
			//read in Q-values from input data files
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
				//if goal state, no need to write into test case because the next state is also the goal state
				if(myWorld.isGoalState(state))
					continue;
				//go through every possible joint action for this state
				for(Action humanAction : Action.values()){
					for(Action robotAction : Action.values()){
						if((MyWorld.mdp.humanAgent.actionsAsList(state).contains(humanAction) || humanAction == Action.WAIT)
								&& (MyWorld.mdp.robotAgent.actionsAsList(state).contains(robotAction) || robotAction == Action.WAIT)){
							State nextState;
							HumanRobotActionPair agentActions;
							do{
								agentActions = new HumanRobotActionPair(humanAction, robotAction);
								//get the next state after the team takes the joint action agentActions
								nextState = myWorld.getNextState(state, agentActions);
								if(humanAction==Action.WAIT && robotAction==Action.WAIT)
									break;
							} while(state.equals(nextState)); //keep looping if the next state == state, which prevents infinite loops in the predefined test cases
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
							
							//read in predefined test cases from input data files
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
