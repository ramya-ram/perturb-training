package code;

import java.util.Random;
import java.util.Scanner;

/**
 * A class specifying all constants and data file directories
 */
public class Constants {
	
	public static final double GAMMA = 1; //gamma is penalty on delayed result
	public static final double ALPHA = 0.05; //learning rate
	public static final double TEMP = 0; //temperature parameter 
	public static final double DELTA_TEMP = 0.01; //change in temperature parameter 
	
	//For PRQL
	public static final double PAST_PROB = 1; //probability of choosing a past policy 
	public static final double DECAY_VALUE = 0.95; //decay the probability of choosing past policies
	
	public static int MAX_TIME = 15; //the number of seconds participants get to make a decision in human subject experiments
	public static final double EPSILON = 0.1; //probability of exploring (vs. exploiting)
	public static double THRESHOLD_SUGG = 0; //threshold for robot to determine whether to suggest or update
	public static double THRESHOLD_ACCEPT = 2; //threshold for robot to determine whether to accept or reject
	
	public static final Random rand = new Random();
	public static final Scanner scan = new Scanner(System.in);
	
	public static final int NUM_EPISODES = 100; //number of episodes agent simulates in the training task before working with the person (works with the person twice for each training task)
	public static final int NUM_EPISODES_TEST = 100; //number of episodes the agent simulates in the testing task before evaluating
	public static final int INTERVAL = 10; //when showing reward over time (Main.INPUT == Main.REWARD_OVER_ITERS), reward is only recorded at every interval (e.g. every 100 iterations)
	public static final int NUM_STEPS_PER_EPISODE = 20; //max number of steps the agent can have in each episode (after this, the agent stops the current execution and goes to the next episode)
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = false;
	public static int NUM_AVERAGING = 3; //run simulations this many times and average to get a more robust result
	public static int NUM_RBM_DATA_POINTS = 100; //number of data points that are sampled and given as input to the RBM (recording ALL might be too much data, so this can be some subset of the total number of <s,a,s'> the agent experiences)
	
	public static int STATES_PER_FIRE = 5;
	public static int NUM_FIRES = 5;
	public static int NONE = 0, HIGHEST = 3, BURNOUT = 4;
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld
	
	public static int NUM_TRAINING_SESSIONS = -1;
	public static int NUM_TESTING_SESSIONS = -1;
	
	//used for human subject experiments
	public static int[] simulationWind_training =    {0, 5, 0}; //robot simulates with an approximate model during experiments
	public static int[] simulationDryness_training = {0, 0, 5};
	public static int[] testWind_training =    {0, 6, 0}; //these are the 'actual' values in the experiment when the robot works with the person
	public static int[] testDryness_training = {0, 0, 6};
	public static int[] simulationWind_testing =    {0, 0, 1, 8};
	public static int[] simulationDryness_testing = {0, 0, 8, 1};
	public static int[] testWind_testing =    {0, 0, 2, 9};
	public static int[] testDryness_testing = {0, 0, 9, 2};
	
	//used for simulation
	public static int[] testWind_training_simulation =    {0, 5, 0};
	public static int[] testDryness_training_simulation = {0, 0, 5};
	public static int[] testWind_testing_simulation =    {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
	public static int[] testDryness_testing_simulation = {0, 9, 8, 7, 6, 5, 4, 3, 2, 1};	

	//directories and file names where input files are stored
	public static String dataDir = "inputFiles\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	//directory where participant data is stored
	public static String participantDir = "C:\\ExperimentData_Dec2014\\";
	
	//directories and file names where simulation results are stored
	public static String simulationDir = "C:\\Extra\\";
	public static String DOMAIN_NAME = "fire";
	public static String rewardLimitedTime = simulationDir+"rewardLimitedTime_"+DOMAIN_NAME+"_"+NUM_AVERAGING+"aver.csv";
	public static String rewardOverIters = simulationDir+"rewardOverIters_"+DOMAIN_NAME+"_"+NUM_AVERAGING+"aver.csv";
	public static String closestTrainingTask = simulationDir+"closestTrainingTask_"+DOMAIN_NAME+"_"+NUM_AVERAGING+"aver.csv";
	
	//directory where Q-values from a participant's training is stored to be used in the participant's testing phase
	public static String trainedQValuesDir = "trainingQValues\\";
}
