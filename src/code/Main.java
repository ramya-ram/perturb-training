package code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sockettest.SocketTest;

public class Main {
	public static int SIMULATION = 0, SIMULATION_HUMAN = 1, ROBOT_HUMAN = 2;
	public static int CURRENT_EXECUTION = SIMULATION;
	
	//socket to send messages to SocketTest
	public static SocketConnect connect;
	
	//parameters
	public static final double SIMULATION_EPSILON = 0.1; // epsilon used in picking an action/how much should be explore
	public static final double HUMAN_EPSILON = 0; // epsilon used in picking an action/how much should be explore

	private static final double GAMMA = 1; // gamma is penalty on delayed result
	private static final double ALPHA = 0.05; // learning rate
	private static final double TEMP = 0.5; //temperature parameter 
	private static final double DELTA_TEMP = 0.1; //change in temperature parameter 
	private static final double PAST_PROB = 1; //probability of choosing a past policy 
	private static final double DECAY_VALUE = 0.95; //decay the probability of choosing past policies
	
	public static int MAX_TIME = 15;
	public static boolean predefined = false;
	public static boolean print = false;
	public static int NUM_AVERAGING = 20;
	public static boolean currWithSimulatedHuman = false;
	
	//num of times to run
	private static final int NUM_EPISODES = 200000;
	public static final int NUM_EPISODES_TEST = 1000;
	private static final int NUM_STEPS_PER_EPISODE = 20;
	
	//file names where results are stored
	public static String storeDataFileBase = "C:\\ComputerSimulationExperimentData\\";
	public static String predefinedPerturb2FileName = storeDataFileBase+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = storeDataFileBase+"predefinedPerturb1.csv";
	public static String predefinedPerturb0FileName = storeDataFileBase+"predefinedPerturb0.csv";
	public static String predefinedProceFileName = storeDataFileBase+"predefinedProce.csv";
	public static String jointQValuesFile = storeDataFileBase+"jointQValuesOffline.csv";
	public static String robotQValuesFile = storeDataFileBase+"robotQValuesOffline.csv";
	public static String participantDir = "C:\\RSS_SimulationResults\\";// + "results1.csv";
	public static String rewardProceName = Main.participantDir+"proceReward_39_93_46_05Train_EntireStates_test11033_1000iter.csv";
	public static String rewardPerturbName = Main.participantDir+"perturbReward_39_93_46_05Train_EntireStates_test11033_1000iter.csv";
	
	public static int humanInteractionNum;
	public static boolean saveToFile;
	public static SocketTest st;
	public static Random rand = new Random(); 
	
	public static double[][][] jointQValuesOffline;
	public static double[][] robotQValuesOffline;
	public static String[][][] perturb2TestCase;
	public static String[][][] perturb1TestCase;
	public static String[][][] perturb0TestCase;
	public static String[][][] proceTestCase;
	
	//prior probabilities for environment variables = wind, dryness
	public static double[] probWind;
	public static double[] probDryness;
	public static double[][] probObsGivenWind;
	public static double[][] probObsGivenDryness;
		
	public static void main(String[] args){		
		initPriorProbabilities();
		
		MyWorld myWorldProce1 = new MyWorld(false, 1);
		MyWorld myWorldProce2 = new MyWorld(false, 2);
		MyWorld myWorldProce3 = new MyWorld(false, 3);

		MyWorld myWorldPerturb1 = new MyWorld(true, 1);
		MyWorld myWorldPerturb2 = new MyWorld(true, 2);
		MyWorld myWorldPerturb3 = new MyWorld(true, 3);
		MyWorld myWorld4 = new MyWorld(true, 4);
		MyWorld myWorld5 = new MyWorld(true, 5);
		MyWorld myWorld6 = new MyWorld(true, 6);
		
		List<MyWorld> trainingWorldsPerturb = new ArrayList<MyWorld>();
		trainingWorldsPerturb.add(myWorldPerturb1);
		trainingWorldsPerturb.add(myWorldPerturb2);
		trainingWorldsPerturb.add(myWorldPerturb3);		
		
		try {
			populateOfflineQValues();
			if(predefined)
				readInPredefinedTestCases();
			
			saveToFile = true;
			
			humanInteractionNum = 0;
			
			for(int i=0; i<NUM_AVERAGING; i++){
				myWorld4.calculateTestSimulationWindDryness();
				myWorld5.calculateTestSimulationWindDryness();
				myWorld6.calculateTestSimulationWindDryness();
				
				//PROCEDURAL
				QLearner qLearnerProce = new QLearner(connect, GAMMA, ALPHA, null, true);
				System.out.println("Training Session 1");
				qLearnerProce.run(myWorldProce1, NUM_EPISODES, NUM_STEPS_PER_EPISODE, "Procedural Training", 
						false /*withHuman*/, false /*computePolicy*/);
				qLearnerProce.run(myWorldProce1, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);
				
				System.out.println("Training Session 2");
				qLearnerProce.run(myWorldProce2, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);
				qLearnerProce.run(myWorldProce2, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);
				
				System.out.println("Training Session 3");
				qLearnerProce.run(myWorldProce3, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);
				qLearnerProce.run(myWorldProce3, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);			
				//qLearnerProce.numOfNonZeroQValues();
	
				//testing session 1
				QLearner qLearnerTest1 = new QLearner(connect, GAMMA, ALPHA, new QValuesSet(qLearnerProce.robotQValues, qLearnerProce.jointQValues), false);
				qLearnerTest1.run(myWorld4, NUM_EPISODES_TEST, NUM_STEPS_PER_EPISODE, null, false, false);
				qLearnerTest1.run(myWorld4, 1, NUM_STEPS_PER_EPISODE, null, true, false);
		
				//testing session 2
				QLearner qLearnerTest2 = new QLearner(connect, GAMMA, ALPHA, new QValuesSet(qLearnerProce.robotQValues, qLearnerProce.jointQValues), false);
				qLearnerTest2.run(myWorld5, NUM_EPISODES_TEST, NUM_STEPS_PER_EPISODE, null, false, false);
				qLearnerTest2.run(myWorld5, 1, NUM_STEPS_PER_EPISODE, null, true, false);
				
				//testing session 3
				QLearner qLearnerTest3 = new QLearner(connect, GAMMA, ALPHA, new QValuesSet(qLearnerProce.robotQValues, qLearnerProce.jointQValues), false);
				qLearnerTest3.run(myWorld6, NUM_EPISODES_TEST, NUM_STEPS_PER_EPISODE, null, false, false);
				qLearnerTest3.run(myWorld6, 1, NUM_STEPS_PER_EPISODE, null, true, false);
				
				//PERTURBATION
				PolicyLibrary policyLibraryPerturbation = new PolicyLibrary();		
				QLearner qLearnerPerturb0 = new QLearner(connect, GAMMA, ALPHA, null, true);
				
				System.out.println("Training Session 1");
				//perturbation training session 1
				qLearnerPerturb0.run(myWorldPerturb1, NUM_EPISODES, NUM_STEPS_PER_EPISODE, "Perturbation Training", false, false);
				Policy perturb1c = qLearnerPerturb0.run(myWorldPerturb1, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, true);		
				//qLearnerPerturb.numOfNonZeroQValues();
				
				QLearner qLearnerPerturb1 = new QLearner(connect, GAMMA, ALPHA, 
						new QValuesSet(qLearnerPerturb0.robotQValues, qLearnerPerturb0.jointQValues), false);
				System.out.println("Training Session 2");
				qLearnerPerturb1.run(myWorldPerturb2, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);
				Policy perturb2c = qLearnerPerturb1.run(myWorldPerturb2, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, true);
				//qLearnerPerturb1.numOfNonZeroQValues();
	
				QLearner qLearnerPerturb2 = new QLearner(connect, GAMMA, ALPHA, 
						new QValuesSet(qLearnerPerturb0.robotQValues, qLearnerPerturb0.jointQValues), false);
				System.out.println("Training Session 3");
				qLearnerPerturb2.run(myWorldPerturb3, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, false);
				Policy perturb3c = qLearnerPerturb2.run(myWorldPerturb3, NUM_EPISODES, NUM_STEPS_PER_EPISODE, null, false, true);	
				//qLearnerPerturb2.numOfNonZeroQValues();
	
				policyLibraryPerturbation.add(perturb1c);
				policyLibraryPerturbation.add(perturb2c);
				policyLibraryPerturbation.add(perturb3c);
	
				//testing session 1
				double[] priorProbs = calculatePrior(trainingWorldsPerturb, myWorld4);
				int maxPolicy1 = calculateMax(priorProbs);				
				QLearner test1Prior = null;
				if(maxPolicy1 == 0)
					test1Prior = qLearnerPerturb0;
				else if(maxPolicy1 == 1)
					test1Prior = qLearnerPerturb1;
				else if(maxPolicy1 == 2)
					test1Prior = qLearnerPerturb2;
				System.out.println("maxpolicy1 "+maxPolicy1);
				PolicyReuseLearner reuseQLearningPerturbation = new PolicyReuseLearner(myWorld4, connect, GAMMA, ALPHA, policyLibraryPerturbation,
						new QValuesSet(test1Prior.robotQValues, test1Prior.jointQValues), priorProbs);
				reuseQLearningPerturbation.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "before", print);
				reuseQLearningPerturbation.policyReuse(TEMP, DELTA_TEMP, NUM_EPISODES_TEST, NUM_STEPS_PER_EPISODE, PAST_PROB, 
						DECAY_VALUE, GAMMA, ALPHA, null, false, false);
				reuseQLearningPerturbation.policyReuse(TEMP, DELTA_TEMP, 1, NUM_STEPS_PER_EPISODE, PAST_PROB, 
						DECAY_VALUE, GAMMA, ALPHA, null, true, false);
				
				//testing session 2
				double[] priorProbs2 = calculatePrior(trainingWorldsPerturb, myWorld5);
				int maxPolicy2 = calculateMax(priorProbs2);
								
				QLearner test2Prior = null;
				if(maxPolicy2 == 0)
					test2Prior = qLearnerPerturb0;
				else if(maxPolicy2 == 1)
					test2Prior = qLearnerPerturb1;
				else if(maxPolicy2 == 2)
					test2Prior = qLearnerPerturb2;
				System.out.println("maxpolicy2 "+maxPolicy2);
				PolicyReuseLearner reuseQLearningPerturbation2 = new PolicyReuseLearner(myWorld5, connect, GAMMA, ALPHA,  policyLibraryPerturbation, 
						new QValuesSet(test2Prior.robotQValues, test2Prior.jointQValues), priorProbs2);
				reuseQLearningPerturbation2.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "before", print);
				reuseQLearningPerturbation2.policyReuse(TEMP, DELTA_TEMP, NUM_EPISODES_TEST, NUM_STEPS_PER_EPISODE, 
						PAST_PROB, DECAY_VALUE, GAMMA, ALPHA, null, false, false);
				reuseQLearningPerturbation2.policyReuse(TEMP, DELTA_TEMP, 1, NUM_STEPS_PER_EPISODE, 
						PAST_PROB, DECAY_VALUE, GAMMA, ALPHA, null, true, false);
	
				//testing session 3
				double[] priorProbs3 = calculatePrior(trainingWorldsPerturb, myWorld6);
				int maxPolicy3 = calculateMax(priorProbs3);
				
				QLearner test3Prior = null;
				if(maxPolicy3 == 0)
					test3Prior = qLearnerPerturb0;
				else if(maxPolicy3 == 1)
					test3Prior = qLearnerPerturb1;
				else if(maxPolicy3 == 2)
					test3Prior = qLearnerPerturb2;
				System.out.println("maxpolicy3 "+maxPolicy3);
				PolicyReuseLearner reuseQLearningPerturbation3 = new PolicyReuseLearner(myWorld6, connect, GAMMA, ALPHA,  policyLibraryPerturbation, 
						new QValuesSet(test3Prior.robotQValues, test3Prior.jointQValues), priorProbs3);
				reuseQLearningPerturbation3.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "before", print);
				reuseQLearningPerturbation3.policyReuse(TEMP, DELTA_TEMP, NUM_EPISODES_TEST, NUM_STEPS_PER_EPISODE, 
						PAST_PROB, DECAY_VALUE, GAMMA, ALPHA, null, false, false);
				reuseQLearningPerturbation3.policyReuse(TEMP, DELTA_TEMP, 1, NUM_STEPS_PER_EPISODE, 
						PAST_PROB, DECAY_VALUE, GAMMA, ALPHA, null, true, false);
				
				BufferedWriter rewardPerturbWriter = new BufferedWriter(new FileWriter(new File(Main.rewardPerturbName), true));
				BufferedWriter rewardProceWriter = new BufferedWriter(new FileWriter(new File(Main.rewardProceName), true));
				rewardPerturbWriter.write("\n");
				rewardProceWriter.write("\n");
				rewardPerturbWriter.close();
				rewardProceWriter.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
//		calculatePrior(trainingWorldsPerturb, myWorld4);
//		calculatePrior(trainingWorldsPerturb, myWorld5);
//		calculatePrior(trainingWorldsPerturb, myWorld6);
	}
	
	public static int calculateMax(double[] arr){
		double maxValue = Integer.MIN_VALUE;
		int maxIndex = -1;
		for(int i=0; i<arr.length; i++){
			System.out.println(""+arr[i]);
			if(arr[i] > maxValue){
				maxValue = arr[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
	
	/**
	 * Initialize the prior probabilities of wind and dryness occurring 
	 * and the conditional probabilities of the observation being correct given the real values.
	 */
	public static void initPriorProbabilities(){
		probWind = new double[10];
		probDryness = new double[10];
		for(int i=0; i<probWind.length; i++){
			if(i==0 || i==1 || i==8 || i==9){
				probWind[i] = 0.05;
				probDryness[i] = 0.05;
			} else if(i==2 || i==7){
				probWind[i] = 0.1;
				probDryness[i] = 0.1;
			} else{
				probWind[i] = 0.15;
				probDryness[i] = 0.15;
			}
		}
		
		probObsGivenWind = new double[10][10];
		probObsGivenDryness = new double[10][10];
		
		for(int i=0; i<probObsGivenWind.length; i++){
			for(int j=0; j<probObsGivenWind[i].length; j++){
				if(i==j)
					probObsGivenWind[i][j] = 0.4;
				else if(Math.abs(i-j) == 1)
					probObsGivenWind[i][j] = 0.2;
				else if(Math.abs(i-j) == 2)
					probObsGivenWind[i][j] = 0.1;
				//else
					//probObsGivenWind[i][j] = 0.01;
			}
		}
		for(int j=0; j<probObsGivenWind[0].length; j++){
			double sum = 0;
			for(int i=0; i<probObsGivenWind.length; i++){
				sum+=probObsGivenWind[i][j];
			}
			sum *= 100;
			sum = Math.round(sum);
			sum /= 100;
			for(int i=0; i<probObsGivenWind.length; i++){
				probObsGivenWind[i][j] /= sum;
			}
		}
		
		for(int i=0; i<probObsGivenWind.length; i++){
			for(int j=0; j<probObsGivenWind[i].length; j++){
				probObsGivenDryness[i][j] = probObsGivenWind[i][j];
				System.out.print(probObsGivenDryness[i][j]+" ");
			}
			System.out.println();
		}
	}
	
	/**
	 * Given the training scenarios and sensor observations of the new scenario, 
	 * the robot tries to determine what's the probability of each training scenario being relevant to the new one.
	 */
	public static double[] calculatePrior(List<MyWorld> trainingWorlds, MyWorld testWorld) {
		//testWorld.setWindAndDryness();
		int[][] trainingRealValues = new int[trainingWorlds.size()][MyWorld.NUM_VARIABLES];
		for(int i=0; i<trainingWorlds.size(); i++){
			trainingRealValues[i][0] = trainingWorlds.get(i).simulationWind;
			trainingRealValues[i][1] = trainingWorlds.get(i).simulationDryness;
			System.out.println(i+ " wind = "+trainingRealValues[i][0]);
			System.out.println(i+ " dryness = "+trainingRealValues[i][1]);
		}
		
		//System.out.println("obsWind "+obsWind+" obsDryness "+obsDryness);
		double[] probs = new double[trainingWorlds.size()];
		for(int i=0; i<trainingWorlds.size(); i++){
			//P(Ow|w)P(w)
			double numWind = probObsGivenWind[testWorld.simulationWind][trainingRealValues[i][0]] * probWind[trainingRealValues[i][0]];
			//System.out.println("numWind "+numWind);
			double denomWind = 0;
			for(int index=0; index<probObsGivenWind.length; index++)
				denomWind += probObsGivenWind[testWorld.simulationWind][index] * probWind[index];
			//System.out.println("denomWind "+denomWind);
			
			double numDryness = probObsGivenDryness[testWorld.simulationDryness][trainingRealValues[i][1]] * probDryness[trainingRealValues[i][1]];
			//System.out.println("numDryness "+numDryness);
			double denomDryness = 0;
			for(int index=0; index<probObsGivenDryness.length; index++)
				denomDryness += probObsGivenDryness[testWorld.simulationDryness][index] * probDryness[index];
			//System.out.println("denomDryness "+denomDryness);
			
			double probOfWind = denomWind>0?(numWind/denomWind):numWind;
			double probOfDryness = denomDryness>0?(numDryness/denomDryness):numDryness;
			probs[i] = probOfWind * probOfDryness;
		}
		double sum=0;
		for(int i=0; i<probs.length; i++)
			sum+=probs[i];
		for(int i=0; i<probs.length; i++){
			if(sum>0)
				probs[i]/=sum;
			probs[i]*=100;
		}
		
		return probs;
	}
	
	/**
	 * Initialize q values from offline learning (saved in a file)
	 */
	public static void populateOfflineQValues(){
		try{
			jointQValuesOffline = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];//new HashMap<StateJointActionPair, Double>();
			BufferedReader jointReader = new BufferedReader(new FileReader(new File(jointQValuesFile)));
			String[] jointValues = jointReader.readLine().split(",");
			
			robotQValuesOffline = new double[MyWorld.mdp.states.size()][Action.values().length];
			BufferedReader robotReader = new BufferedReader(new FileReader(new File(robotQValuesFile)));
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
			
			BufferedReader readerProce = new BufferedReader(new FileReader(new File(Main.predefinedProceFileName)));
			String[] nextStatesProce = readerProce.readLine().split(",");
			readerProce.close();
			
			BufferedReader readerPerturb0 = new BufferedReader(new FileReader(new File(Main.predefinedPerturb0FileName)));
			String[] nextStatesPerturb0 = readerPerturb0.readLine().split(",");
			readerPerturb0.close();
			
			BufferedReader readerPerturb1 = new BufferedReader(new FileReader(new File(Main.predefinedPerturb1FileName)));
			String[] nextStatesPerturb1 = readerPerturb1.readLine().split(",");
			readerPerturb1.close();
			
			BufferedReader readerPerturb2 = new BufferedReader(new FileReader(new File(Main.predefinedPerturb2FileName)));
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
