package code;

/**
 * A class specifying all constants and data file directories
 */
public class Constants {
	
	public static final double GAMMA = 1; // gamma is penalty on delayed result
	public static final double ALPHA = 0.05; // learning rate
	public static final double TEMP = 0; //temperature parameter 
	public static final double DELTA_TEMP = 0.1; //change in temperature parameter 
	
	public static int MAX_TIME = 15;
	public static final double EPSILON = 0.1;
	
	public static double THRESHOLD_SUGG = 0;
	public static double THRESHOLD_REJECT = 2;
	
	//num of times to run
	public static final int NUM_EPISODES = 100000;
	public static final int NUM_EPISODES_TEST = 1000;
	public static final int NUM_STEPS_PER_EPISODE = 20; 
	
	public static final int NUM_EPISODES_PRUNING = 100;
	public static final int PRUNING_THRESHOLD = 20;
	
	public static final int NUM_TRAINING_SESSIONS = 3;
	public static final int NUM_TESTING_SESSIONS = 3;
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = true;
	public static boolean print = false;
	public static int NUM_AVERAGING = 20;
	
	public static int STATES_PER_FIRE = 5;
	public static int NUM_FIRES = 5;
	public static int NONE = 0, HIGHEST = 3, BURNOUT = 4;
	public static int indexOfFireInAction = 7; //PUT_OUT[0,1,2,3,4] -- the fire number is at the 7th index
	public static int NUM_VARIABLES = 2; //wind + dryness = 2
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1;
	
	//file names where results are stored
	public static String dataDir = "C:\\ExperimentData_Dec2014\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedPerturb0FileName = dataDir+"predefinedPerturb0.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	public static String participantDir = dataDir;
	
	public static String simulationDir = "C:\\RSS_SimulationResults_Pruning\\";
	public static String rewardProceQName = simulationDir+"proceQReward_29_92_39_Train60_06_"+NUM_EPISODES_TEST+"iter_prune"+NUM_EPISODES_PRUNING+"_thres"+PRUNING_THRESHOLD+"_currPolicyCanRemove.csv";
	public static String rewardPerturbQName = simulationDir+"perturbQReward_29_92_39_Train60_06_"+NUM_EPISODES_TEST+"iter_prune"+NUM_EPISODES_PRUNING+"_thres"+PRUNING_THRESHOLD+"_currPolicyCanRemove.csv";
	public static String rewardHRPRName = simulationDir+"HRPRReward_29_92_39_Train60_06_"+NUM_EPISODES_TEST+"iter_prune"+NUM_EPISODES_PRUNING+"_thres"+PRUNING_THRESHOLD+"_currPolicyCanRemove.csv";
}
