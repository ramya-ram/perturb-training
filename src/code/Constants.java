package code;

/**
 * A class specifying all constants and data file directories
 */
public class Constants {
	
	public static final double GAMMA = 1; // gamma is penalty on delayed result
	public static final double ALPHA = 0.05; // learning rate
	public static final double TEMP = 0; //temperature parameter 
	public static final double DELTA_TEMP = 0.01; //change in temperature parameter 
	
	public static int MAX_TIME = 15;
	public static final double EPSILON = 0.1;
	
	public static double THRESHOLD_SUGG = 0;
	public static double THRESHOLD_REJECT = 2;
	
	//num of times to run
	public static final int NUM_EPISODES = 200000;
	public static final int NUM_EPISODES_TEST = 1500;
	public static final int NUM_STEPS_PER_EPISODE = 20; 
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = true;
	public static boolean print = false;
	public static int NUM_AVERAGING = 25;
	
	public static int STATES_PER_FIRE = 5;
	public static int NUM_FIRES = 5;
	public static int NONE = 0, HIGHEST = 3, BURNOUT = 4;
	public static int indexOfFireInAction = 7; //PUT_OUT[0,1,2,3,4] -- the fire number is at the 7th index
	public static int NUM_VARIABLES = 2; //wind + dryness = 2
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld
	
	public static int[] simulationWind_train =    {0, 5, 0};
	public static int[] simulationDryness_train = {0, 0, 5};
	
	public static int[] testWind_train =    {0, 6, 0};
	public static int[] testDryness_train = {0, 0, 6};
	
	public static int[] simulationWind_test =    {0, 1, 8};
	public static int[] simulationDryness_test = {0, 8, 1};
	
	public static int[] testWind_test =    {0, 2, 9};
	public static int[] testDryness_test = {0, 9, 2};	
	
	public static final int NUM_TRAINING_SESSIONS = testWind_train.length;
	public static final int NUM_TESTING_SESSIONS = testWind_test.length;

	//file names where results are stored
	public static String dataDir = "inputFiles\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	public static String participantDir = "C:\\ExperimentData_Dec2014\\";
	
	public static String simulationDir = "C:\\Extra\\";
	public static String rewardProceQName = simulationDir+"PQ_train00_06_60_"+testWind_test[0]+testDryness_test[0]+"_"+testWind_test[1]+testDryness_test[1]+"_"+testWind_test[2]+testDryness_test[2]+".csv";
	public static String rewardPerturbQName = simulationDir+"BQ_train00_06_60_"+testWind_test[0]+testDryness_test[0]+"_"+testWind_test[1]+testDryness_test[1]+"_"+testWind_test[2]+testDryness_test[2]+".csv";
	public static String rewardHRPRName = simulationDir+"BH_train00_06_60_"+testWind_test[0]+testDryness_test[0]+"_"+testWind_test[1]+testDryness_test[1]+"_"+testWind_test[2]+testDryness_test[2]+".csv";

	public static String qvaluesDir = simulationDir;
	
	public static String trainedQValuesDir = "C:\\Users\\julie\\Dropbox (MIT)\\trainingQValues\\";
}
