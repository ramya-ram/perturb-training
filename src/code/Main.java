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
	public static int SIMULATION = 0, SIMULATION_HUMAN = 1, ROBOT_HUMAN = 2, CREATE_PREDEFINED = 3;
	public static int CURRENT_EXECUTION = ROBOT_HUMAN;
	
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
	
	public static void main(String[] args){	
		
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
		
		if(CURRENT_EXECUTION == CREATE_PREDEFINED){
			writePredefinedTestCase(testingWorlds.get(0), Constants.predefinedProceFileName);
			//proceTestCase = readPredefinedTestCase(Constants.predefinedProceFileName);
			writePredefinedTestCase(testingWorlds.get(1), Constants.predefinedPerturb1FileName);
			writePredefinedTestCase(testingWorlds.get(2), Constants.predefinedPerturb2FileName);
		}
		
		try {
			if(Constants.useOfflineValues)
				populateOfflineQValues();
			if(Constants.usePredefinedTestCases)
				readInPredefinedTestCases();
			saveToFile = true;
						
			if(CURRENT_EXECUTION == SIMULATION){
				for(int i=0; i<Constants.NUM_AVERAGING; i++){
					//makes simulation wind and dryness a noisy version of the real one
					System.out.println("NEW simulation");
					for(MyWorld trainWorld : trainingWorldsProce)
						trainWorld.calculateSimulationWindDryness();
					for(MyWorld trainWorld : trainingWorldsPerturb)
						trainWorld.calculateSimulationWindDryness();
					for(MyWorld testWorld : testingWorlds)
						testWorld.calculateSimulationWindDryness();
					
					//PROCEDURAL - Q-learning
					TaskExecution proceQ = new TaskExecution(null, trainingWorldsProce, testingWorlds, ExperimentCondition.PROCE_Q);
					proceQ.executeTask();
					//TODO: make sure the human sessions are run for only 1 episode
					
					//PERTURBATION - Q-learning
					TaskExecution perturbQ = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.PERTURB_Q);
					perturbQ.executeTask();
					
					//PERTURBATION - HRPR
					TaskExecution HRPR = new TaskExecution(null, trainingWorldsPerturb, testingWorlds, ExperimentCondition.HRPR);
					HRPR.executeTask();
					
					BufferedWriter rewardHRPRWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardHRPRName), true));
					BufferedWriter rewardPerturbQWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPerturbQName), true));
					BufferedWriter rewardProceQWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardProceQName), true));
					
					rewardHRPRWriter.write("\n");
					rewardPerturbQWriter.write("\n");
					rewardProceQWriter.write("\n");
					
					rewardHRPRWriter.close();
					rewardPerturbQWriter.close();
					rewardProceQWriter.close();
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
				if(CURRENT_EXECUTION == ROBOT_HUMAN){
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
					TaskExecution HRPR = new TaskExecution(gameView, trainingWorldsPerturb, testingWorlds, ExperimentCondition.HRPR);
					HRPR.executeTask();
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
			int statesPerFire = Constants.STATES_PER_FIRE;
	        for(int i=0; i<statesPerFire; i++){
				for(int j=0; j<statesPerFire; j++){
					for(int k=0; k<statesPerFire; k++){
						for(int l=0; l<statesPerFire; l++){
							for(int m=0; m<statesPerFire; m++){
								int[] stateOfFires = {i,j,k,l,m};
								State state = new State(stateOfFires);													
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
						}
					}
				}
			}     
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void writePredefinedTestCase(MyWorld myWorld, String fileName){
		try {
			int num = 0;
			System.out.println("in populate");
			BufferedWriter stateWriter = new BufferedWriter(new FileWriter(new File(fileName), true));
			int statesPerFire = Constants.STATES_PER_FIRE;

			for(int i=0; i<statesPerFire; i++){
				for(int j=0; j<statesPerFire; j++){
					for(int k=0; k<statesPerFire; k++){
						for(int l=0; l<statesPerFire; l++){
							for(int m=0; m<statesPerFire; m++){
								int[] stateOfFires = {i,j,k,l,m};
								State state = new State(stateOfFires);
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
												nextState = myWorld.computePredefinedNextState(state, agentActions);
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
	public static void readInPredefinedTestCases(){
		try{
			proceTestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			perturb1TestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			perturb2TestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			
			BufferedReader readerProce = new BufferedReader(new FileReader(new File(Constants.predefinedProceFileName)));
			String[] nextStatesProce = readerProce.readLine().split(",");
			readerProce.close();
			
			BufferedReader readerPerturb1 = new BufferedReader(new FileReader(new File(Constants.predefinedPerturb1FileName)));
			String[] nextStatesPerturb1 = readerPerturb1.readLine().split(",");
			readerPerturb1.close();
			
			BufferedReader readerPerturb2 = new BufferedReader(new FileReader(new File(Constants.predefinedPerturb2FileName)));
			String[] nextStatesPerturb2 = readerPerturb2.readLine().split(",");
			readerPerturb2.close();
			
			System.out.println("next states perturb2 size "+nextStatesPerturb2.length); 
			System.out.println("next states perturb1 size "+nextStatesPerturb1.length); 
			System.out.println("next states proce size "+nextStatesProce.length);
			
			int num=0;
		
	        for(int i=0; i<Constants.STATES_PER_FIRE; i++){
				for(int j=0; j<Constants.STATES_PER_FIRE; j++){
					for(int k=0; k<Constants.STATES_PER_FIRE; k++){
						for(int l=0; l<Constants.STATES_PER_FIRE; l++){
							for(int m=0; m<Constants.STATES_PER_FIRE; m++){
								int[] stateOfFires = {i,j,k,l,m};
								State state = new State(stateOfFires);
								if(MyWorld.isGoalState(state))
									continue;
								for(Action humanAction : Action.values()){
									for(Action robotAction : Action.values()){
										if((MyWorld.mdp.humanAgent.actionsAsList(state).contains(humanAction) || humanAction == Action.WAIT)
												&& (MyWorld.mdp.robotAgent.actionsAsList(state).contains(robotAction) || robotAction == Action.WAIT)){
											
											String str1 = nextStatesProce[num];
											if(str1.length() > 0)
												proceTestCase[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = str1;
											
											String str2 = nextStatesPerturb1[num];
											if(str2.length() > 1)
												perturb1TestCase[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = str2;
											
											String str3 = nextStatesPerturb2[num];
											if(str3.length() > 1)
												perturb2TestCase[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = str3;
											num++;
										}
									}
								}
							}
						}
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
