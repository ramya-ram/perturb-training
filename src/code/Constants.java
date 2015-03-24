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
	public static final int NUM_EPISODES = 100000;//500000;
	public static final int NUM_EPISODES_TEST = 100;//1500;
	public static final int NUM_STEPS_PER_EPISODE = 200; 
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = false;
	public static boolean print = false;
	public static int NUM_AVERAGING = 1;
	
	public static int STATES_PER_ITEM = 2;
	public static int NUM_ITEMS = 5;
	public static int NUM_POS = 5;//NUM_ITEMS;
	//public static int NUM_HUMAN_POS = 3;
	public static int indexOfItemInAction = 4; //GET_[0,1,2,3,4] -- the item number is at the 4th index
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld
	
	/*public static int[] simulationWind_train =    {0, 5, 0};
	public static int[] simulationDryness_train = {0, 0, 5};
	
	public static int[] testWind_train =    {0, 6, 0};
	public static int[] testDryness_train = {0, 0, 6};
	
	public static int[] simulationWind_test =    {0, 0, 1, 8};
	public static int[] simulationDryness_test = {0, 0, 8, 1};
	
	public static int[] testWind_test =    {0, 0, 2, 9};
	public static int[] testDryness_test = {0, 0, 9, 2};*/	
	
	public static final int NUM_TRAINING_SESSIONS = 1;//testWind_train.length;
	public static final int NUM_TESTING_SESSIONS = 1;//testWind_test.length;

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
	public static String rewardHRPerturbName = simulationDir+"BH.csv";

	public static String qvaluesDir = simulationDir;
	
	public static String trainedQValuesDir = "C:\\Users\\julie\\Dropbox (MIT)\\trainingQValues\\";
}
