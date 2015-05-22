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
	public static int SIMULATION = 0, //use for running simulation runs on the computer
			SIMULATION_HUMAN_TRAIN_TEST = 1, //use for human experiments where participants work with the simulation environment for training and testing
			SIMULATION_HUMAN_TRAIN = 2, //use for human experiments where participants work with the simulation environment only for training
			ROBOT_HUMAN_TEST = 3, //use for human experiments where participants work with the robot in testing after training in simulation
			
			CREATE_PREDEFINED = 4, //use for creating predefined test cases for human subject experiments (given a state and joint action, the next state will always be the same across participants)
			CREATE_OFFLINE_QVALUES = 5; //use for running offline deterministic simulations and having these values saved to a file so that the robot starts with base knowledge when working with a human
	
	public static int CURRENT_EXECUTION = SIMULATION; //set CURRENT_EXECUTION to one of the above depending on which option you want to run
	
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
	public static double[][] HRPerturbTotal;
	
	public static void main(String[] args){	
		PRQLTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		HRPerturbTotal = new double[Constants.NUM_TESTING_SESSIONS][Constants.NUM_EPISODES_TEST/Constants.INTERVAL];
		
		//construct training worlds for procedural and perturbation
		//List<MyWorld> trainingWorldsProce = new ArrayList<MyWorld>();
		List<MyWorld> trainingWorldsPerturb = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TRAINING_SESSIONS; i++){
			//MyWorld proceWorld = new MyWorld(Constants.TRAINING, false, i);//, Constants.trainingGoalLocs[0], Constants.allTokenLocs.get(0), Constants.allPitLocs.get(0));
			//trainingWorldsProce.add(proceWorld);
			MyWorld perturbWorld = new MyWorld(Constants.TRAINING, true, i);//, Constants.trainingGoalLocs[i-1], Constants.allTokenLocs.get(i-1), Constants.allPitLocs.get(i-1));
			trainingWorldsPerturb.add(perturbWorld);
		}
		//construct testing worlds for both training
		List<MyWorld> testingWorlds = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TESTING_SESSIONS; i++){
			MyWorld testWorld = new MyWorld(Constants.TESTING, true, i);//, Constants.testingGoalLocs[i-1], Constants.allTokenLocsTest.get(i-1), Constants.allPitLocsTest.get(i-1));
			testingWorlds.add(testWorld);
		}
		
		//trainingWorldsPerturb.get(0).printGrid();
		
		if(CURRENT_EXECUTION == CREATE_OFFLINE_QVALUES){
			QLearner qLearnerProce = new QLearner(null, ExperimentCondition.PROCE_Q);
			//qLearnerProce.run(trainingWorldsProce.get(0), false /*withHuman*/);
			qLearnerProce.saveOfflineLearning();
			return;
		}
		
		if(CURRENT_EXECUTION == CREATE_PREDEFINED){
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
				//for(int i=0; i<Constants.NUM_AVERAGING; i++){
					//makes simulation wind and dryness a noisy version of the real one
					//System.out.println("*** "+i+" ***");
					//for(MyWorld trainWorld : trainingWorldsProce)
					//	trainWorld.calculateSimulationWindDryness();
					//for(MyWorld trainWorld : trainingWorldsPerturb)
						//trainWorld.calculateSimulationWindDryness();
					//for(MyWorld trainWorld : trainingWorldsPerturb){
						//trainWorld.changeTokenPitLocs();
						//trainWorld.changeGoalLoc();
						//System.out.println("trainWorld goal loc = "+trainWorld.goalLoc);
					//}
					//for(MyWorld testWorld : testingWorlds){
						//testWorld.changeTokenPitLocs();
						//testWorld.changeGoalLoc();
						//System.out.println("testWorld goal loc = "+testWorld.goalLoc);
					//}
					
					/*//PROCEDURAL - Q-learning
					TaskExecution proceQ = new TaskExecution(null, trainingWorldsProce, testingWorlds, ExperimentCondition.PROCE_Q);
					proceQ.executeTask();
					//TODO: make sure the human sessions are run for only 1 episode
					
					//PERTURBATION - Q-learning
					TaskExecution perturbQ = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PERTURB_Q);
					perturbQ.executeTask();*/
					
					//PERTURBATION - HRPR
					TaskExecution HRPerturb = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.HR_PERTURB);
					HRPerturb.executeTask();
					
					/*BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.numIterName), true));
					rewardWriter.write("\n\n");
					rewardWriter.close();*/
					
					//PERTURBATION - PRQL
					TaskExecution PRQL = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PRQL);
					PRQL.executeTask();
					
					/*rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.numIterName), true));
					rewardWriter.write("\n\n");
					rewardWriter.close();*/
					
					//Standard QLearning
					//TaskExecution QLearning = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.Q_LEARNING);
					//QLearning.executeTask();
					
					/*BufferedWriter rewardHRPerturbWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardHRPerturbName), true));
					//BufferedWriter rewardPerturbQWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPerturbQName), true));
					//BufferedWriter rewardProceQWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardProceQName), true));
					BufferedWriter rewardPRQLWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPRQLName), true));
					BufferedWriter rewardQLearningWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardQLearningName), true));
					
					rewardHRPerturbWriter.write("\n");
					//rewardPerturbQWriter.write("\n");
					//rewardProceQWriter.write("\n");
					rewardPRQLWriter.write("\n");
					rewardQLearningWriter.write("\n");
					
					rewardHRPerturbWriter.close();
					//rewardPerturbQWriter.close();
					//rewardProceQWriter.close();
					rewardPRQLWriter.close();
					rewardQLearningWriter.close();*/
				//}
				BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.numIterName), true));
						
				for(int i=0; i<HRPerturbTotal.length; i++){
				for(int j=0; j<HRPerturbTotal[i].length; j++){
					rewardWriter.write((HRPerturbTotal[i][j]/Constants.NUM_AVERAGING)+", ");
					//rewardHRPerturbWriter.write((HRPerturbTotal[i][j]/Constants.NUM_AVERAGING)+", ");
					//rewardPRQLWriter.write((PRQLTotal[i][j]/Constants.NUM_AVERAGING)+", ");
				}
					rewardWriter.write("\n");
					//rewardPRQLWriter.write("\n");
				}
				rewardWriter.write("\n\n");
				
				for(int i=0; i<PRQLTotal.length; i++){
					for(int j=0; j<PRQLTotal[i].length; j++){
						rewardWriter.write((PRQLTotal[i][j]/Constants.NUM_AVERAGING)+", ");
					}
					rewardWriter.write("\n");
				}
								
				rewardWriter.close();
			} else {	
				//sets simulation wind and dryness
				/*for(MyWorld trainWorld : trainingWorldsProce)
					trainWorld.setSimulationWindDryness(Constants.simulationWind_train[0], Constants.simulationDryness_train[0]);
				for(MyWorld trainWorld : trainingWorldsPerturb)
					trainWorld.setSimulationWindDryness(Constants.simulationWind_train[trainWorld.sessionNum-1], Constants.simulationDryness_train[trainWorld.sessionNum-1]);
				for(MyWorld testWorld : testingWorlds)
					testWorld.setSimulationWindDryness(Constants.simulationWind_test[testWorld.sessionNum-1], Constants.simulationDryness_test[testWorld.sessionNum-1]);*/
				
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
					//TaskExecution proceQ = new TaskExecution(gameView, trainingWorldsProce, testingWorlds, ExperimentCondition.PROCE_Q);
					//proceQ.executeTask();
				} else if(trainingType.equalsIgnoreCase("BQ")){
					//PERTURBATION - Q-learning
					TaskExecution perturbQ = new TaskExecution(gameView, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PERTURB_Q);
					perturbQ.executeTask();
				} else if(trainingType.equalsIgnoreCase("BH")){
					//PERTURBATION
					TaskExecution HRPerturb = new TaskExecution(gameView, trainingWorldsPerturb, testingWorlds, ExperimentCondition.HR_PERTURB);
					HRPerturb.executeTask();
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
				//if(myWorld.isGoalState(state))
				//	continue;
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
