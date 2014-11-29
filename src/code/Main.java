package code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import sockettest.SocketTest;

public class Main {
	public static int SIMULATION = 0, SIMULATION_HUMAN = 1, ROBOT_HUMAN = 2;
	public static int CURRENT_EXECUTION = SIMULATION;
	
	//socket to send messages to SocketTest
	public static SocketConnect connect;
	
	public static boolean currWithSimulatedHuman = false;

	
	public static int humanInteractionNum;
	public static boolean saveToFile;
	public static SocketTest st;
	
	public static double[][][] jointQValuesOffline;
	public static double[][] robotQValuesOffline;
	public static String[][][] perturb2TestCase;
	public static String[][][] perturb1TestCase;
	public static String[][][] perturb0TestCase;
	public static String[][][] proceTestCase;
	
	public static void main(String[] args){	
		
		//construct training worlds for procedural and perturbation
		List<MyWorld> trainingWorldsProce = new ArrayList<MyWorld>();
		List<MyWorld> trainingWorldsPerturb = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TRAINING_SESSIONS; i++){
			MyWorld proceWorld = new MyWorld(false, i);
			trainingWorldsProce.add(proceWorld);
			MyWorld perturbWorld = new MyWorld(true, i);
			trainingWorldsPerturb.add(perturbWorld);
		}
		//construct testing worlds for both training
		List<MyWorld> testingWorlds = new ArrayList<MyWorld>();
		for(int i=1; i<=Constants.NUM_TESTING_SESSIONS; i++){
			MyWorld testWorld = new MyWorld(true, i+Constants.NUM_TRAINING_SESSIONS);
			testingWorlds.add(testWorld);
		}
		
		try {
			populateOfflineQValues();
			if(Constants.predefined)
				readInPredefinedTestCases();
			
			saveToFile = true;
			humanInteractionNum = 0;
			
			for(int i=0; i<Constants.NUM_AVERAGING; i++){				
				//PROCEDURAL
				TaskExecution proce = new TaskExecution(trainingWorldsProce, testingWorlds, false);
				proce.executeTask();
				//TODO: make sure the human sessions are run for only 1 episode
				
				//PERTURBATION
				TaskExecution perturb = new TaskExecution(trainingWorldsPerturb, testingWorlds, true);
				perturb.executeTask();
				
				BufferedWriter rewardPerturbWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPerturbName), true));
				BufferedWriter rewardProceWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardProceName), true));
				rewardPerturbWriter.write("\n");
				rewardProceWriter.write("\n");
				rewardPerturbWriter.close();
				rewardProceWriter.close();
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
	        for(int i=0; i<MyWorld.STATES_PER_FIRE; i++){
				for(int j=0; j<MyWorld.STATES_PER_FIRE; j++){
					for(int k=0; k<MyWorld.STATES_PER_FIRE; k++){
						for(int l=0; l<MyWorld.STATES_PER_FIRE; l++){
							for(int m=0; m<MyWorld.STATES_PER_FIRE; m++){
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
	
	/**
	 * Read in from a file predefined test cases so that participants can be compared fairly on their performance in the testing sessions
	 */
	public static void readInPredefinedTestCases(){
		try{
			proceTestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			perturb0TestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			perturb1TestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			perturb2TestCase = new String[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			
			BufferedReader readerProce = new BufferedReader(new FileReader(new File(Constants.predefinedProceFileName)));
			String[] nextStatesProce = readerProce.readLine().split(",");
			readerProce.close();
			
			BufferedReader readerPerturb0 = new BufferedReader(new FileReader(new File(Constants.predefinedPerturb0FileName)));
			String[] nextStatesPerturb0 = readerPerturb0.readLine().split(",");
			readerPerturb0.close();
			
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
	        for(int i=0; i<MyWorld.STATES_PER_FIRE; i++){
				for(int j=0; j<MyWorld.STATES_PER_FIRE; j++){
					for(int k=0; k<MyWorld.STATES_PER_FIRE; k++){
						for(int l=0; l<MyWorld.STATES_PER_FIRE; l++){
							for(int m=0; m<MyWorld.STATES_PER_FIRE; m++){
								int[] stateOfFires = {i,j,k,l,m};
								State state = new State(stateOfFires);
								if(MyWorld.isGoalState(state))
									continue;
								for(Action humanAction : Action.values()){
									for(Action robotAction : Action.values()){
										if((MyWorld.mdp.humanAgent.actionsAsList(state).contains(humanAction) || humanAction == Action.WAIT)
												&& (MyWorld.mdp.robotAgent.actionsAsList(state).contains(robotAction) || robotAction == Action.WAIT)){
											//StateJointActionPair pair = new StateJointActionPair(state, new HumanRobotActionPair(humanAction, robotAction));
											
											String str1 = nextStatesProce[num];
											if(str1.length() > 0)
												proceTestCase[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = str1;
											
											String str0 = nextStatesPerturb0[num];
											if(str0.length() > 1)
												perturb0TestCase[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = str0;
											
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
