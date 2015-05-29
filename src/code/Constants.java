package code;

/**
 * A class specifying all constants and data file directories
 */
public class Constants {
	
	public static final double GAMMA = 1; // gamma is penalty on delayed result
	public static final double ALPHA = 0.05; // learning rate
	public static final double TEMP = 0; //temperature parameter 
	public static final double DELTA_TEMP = 0.01; //change in temperature parameter 
	
	//For PRQL
	public static final double PAST_PROB = 1; //probability of choosing a past policy 
	public static final double DECAY_VALUE = 0.95; //decay the probability of choosing past policies
	
	public static int MAX_TIME = 15;
	public static final double EPSILON = 0.1;
	
	public static double THRESHOLD_SUGG = 0;
	public static double THRESHOLD_REJECT = 2;
	
	//num of times to run
	public static final int NUM_EPISODES = 500000;
	public static final int NUM_EPISODES_TEST = 50000;
	public static final int NUM_STEPS_PER_EPISODE = 100;
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = false;
	public static boolean print = false;
	public static final int INTERVAL = 100;
	public static int NUM_AVERAGING = 20;
	
	public static int OBSTACLE_COL = 1; 
	
	public static int NUM_ROWS = 3;
	public static int NUM_COLS = 3;
	
	public static int NUM_ITEMS = 3;
	public static int NUM_LOCS_OBSTACLE = NUM_COLS;
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld

	public static final int NUM_TRAINING_SESSIONS = 3;
	public static final int NUM_TESTING_SESSIONS = 1;
	
	public static int[][] trainingGoalItems = {{0,3,3},
										{3,3,0},
										{3,0,3}};
	public static int[][] testingGoalItems = {{2,1,4},
									   {2,2,2},
									   {3,4,1}};

	//file names where results are stored
	public static String dataDir = "inputFiles\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	public static String participantDir = "C:\\ExperimentData_Dec2014\\";
	
	public static String simulationDir = "C:\\Extra\\";
	public static String rewardProceQName = simulationDir+"PQ.csv";
	public static String rewardPerturbQName = simulationDir+"BQ.csv";
	public static String rewardHRPerturbName = simulationDir+"BH_delivery_500000train_10000test.csv";
	public static String rewardPRQLName = simulationDir+"PRQL_delivery_500000train_10000test.csv";
	public static String rewardQLearningName = simulationDir+"QLearning_delivery_500000train_10000test.csv";
	
	public static String numIterName = simulationDir+"numIter_reward_deliverywithPerturbs_changingGoalState_100interval_til50000_20aver.csv";
	
	public static String qvaluesDir = simulationDir;
	
	public static String trainedQValuesDir = "C:\\Users\\julie\\Dropbox (MIT)\\trainingQValues\\";
}
