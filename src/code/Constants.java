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
	public static final int NUM_EPISODES = 200000;
	public static final int NUM_EPISODES_TEST = 1500;
	public static final int NUM_STEPS_PER_EPISODE = 20; 
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = false;
	public static boolean print = false;
	public static int NUM_AVERAGING = 50;
	
	public static int STATES_PER_PART = 3;
	public static int NUM_PARTS = 10;
	public static int NONE = 0, PARTIAL = 1, COMPLETE = 2;
	public static int indexOfPartInAction = 4; //PUT_[0,1,2,3,4] -- the part number is at the 4th index
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld

	public static final int[][] trainingSeqs = {{3,4,2,5,1,6,7,8,9,0},
												{1,6,7,8,9,0,3,4,2,5},
												{3,4,2,8,9,0,5,1,6,7}};
	
	public static final int[][] testingSeqs = {{3,4,8,9,0,2,5,1,6,7},
											   {8,9,0,3,4,2,5,1,6,7},
											   {0,5,1,6,3,4,2,8,9,7}};
	
	public static final int NUM_TRAINING_SESSIONS = trainingSeqs.length;
	public static final int NUM_TESTING_SESSIONS = testingSeqs.length;
	
	//file names where results are stored
	public static String dataDir = "inputFiles\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	public static String participantDir = "C:\\ExperimentData_Dec2014\\";
	
	public static String simulationDir = "C:\\Extra\\";
	public static String rewardProceQName = simulationDir+"PQ_3Policies_50Aver_NoNoise.csv";
	public static String rewardPerturbQName = simulationDir+"BQ_3Policies_50Aver_NoNoise.csv";
	public static String rewardHRPerturbName = simulationDir+"BH_3Policies_50Aver_NoNoise.csv";
	public static String rewardPRQLName = simulationDir+"PRQL_3Policies_50Aver_NoNoise.csv";
	
	public static String qvaluesDir = simulationDir;
	
	public static String trainedQValuesDir = "C:\\Users\\julie\\Dropbox (MIT)\\trainingQValues\\";
}
