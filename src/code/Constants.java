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
	public static final int NUM_EPISODES_TEST = 5000;
	public static final int NUM_STEPS_PER_EPISODE = 30;
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = false;
	public static boolean print = false;
	public static int NUM_AVERAGING = 1;
	
	public static int NUM_ROWS = 10;
	public static int NUM_COLS = 10;
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld

	public static Location[] trainingGoalLocs = {new Location(0,0), new Location(NUM_ROWS-1, 0), new Location(0,NUM_COLS-1), new Location(NUM_ROWS-1, NUM_COLS-1)};
	/*public static Location[] testingGoalLocs = {new Location(2,NUM_COLS-2), new Location(NUM_ROWS-2, 2), new Location(NUM_ROWS-3, NUM_COLS-1)};
	*/
	public static final int NUM_TRAINING_SESSIONS = 4;
	public static final int NUM_TESTING_SESSIONS = 100;

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
	public static String rewardHRPerturbName = simulationDir+"BH_tokensAndGoal_500000train_5000test_100TestLocs_changedTokenLocs.csv";
	public static String rewardPRQLName = simulationDir+"PRQL_tokensAndGoal_500000train_5000test_100TestLocs_changedTokenLocs.csv";
	public static String rewardQLearningName = simulationDir+"QLearning_tokensAndGoal_500000train_5000test_100TestLocs_changedTokenLocs.csv";
	
	public static String qvaluesDir = simulationDir;
	
	public static String trainedQValuesDir = "C:\\Users\\julie\\Dropbox (MIT)\\trainingQValues\\";
}
