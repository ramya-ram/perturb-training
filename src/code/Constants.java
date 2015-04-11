package code;

import java.util.Arrays;
import java.util.List;

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
	public static final int NUM_EPISODES_TEST = 3000;
	public static final int NUM_STEPS_PER_EPISODE = 40;
	
	public static boolean usePredefinedTestCases = false;
	public static boolean useOfflineValues = false;
	public static boolean print = false;
	public static int NUM_AVERAGING = 50;
	
	public static int NUM_ROWS = 10;
	public static int NUM_COLS = 10;
	
	public static int HUMAN = 0, ROBOT = 1;
	public static int TRAINING = 0, TESTING = 1, PRACTICE = 2; //typeOfWorld

	public static Location[] trainingGoalLocs = {new Location(0,NUM_COLS-1), new Location(NUM_ROWS-1, 0), new Location(NUM_ROWS-1, NUM_COLS-1)};
	public static Location[] testingGoalLocs = {new Location(2,NUM_COLS-2), new Location(NUM_ROWS-2, 2), new Location(NUM_ROWS-3, NUM_COLS-1)};
	
	public static List<Location> tokenLocs1 = Arrays.asList(new Location(3,3), new Location(2,4), new Location(8,8), new Location(6,7), new Location(7,2), new Location(1,8), new Location(2,8), new Location(3,8),
			new Location(5,5), new Location(1,1), new Location(2,0));
	public static List<Location> pitLocs1 = Arrays.asList(new Location(2,3), new Location(3,4), new Location(5,6), new Location(6,6), new Location(9,1), new Location(8,9));
	
	public static List<Location> tokenLocs2 = Arrays.asList(new Location(0,0), new Location(2,4), new Location(8,8), new Location(6,7), new Location(7,2), new Location(1,6), new Location(2,6), new Location(3,6),
			new Location(9,8), new Location(5,5), new Location(7,0));
	public static List<Location> pitLocs2 = Arrays.asList(new Location(2,3), new Location(3,4), new Location(0,2), new Location(6,6), new Location(7,1), new Location(7,9));
	
	public static List<Location> tokenLocs3 = Arrays.asList(new Location(0,0), new Location(2,4), new Location(5,2), new Location(5,3), new Location(5,4), new Location(1,6), new Location(2,6), new Location(3,6),
			new Location(9,8), new Location(5,5), new Location(7,0));
	public static List<Location> pitLocs3 = Arrays.asList(new Location(1,1), new Location(3,4), new Location(7,6), new Location(7,7), new Location(7,8), new Location(7,9));
	
	public static List<Location> tokenLocsTest1 = Arrays.asList(new Location(1,2), new Location(1,3), new Location(1,4), new Location(5,5), new Location(5,4), new Location(1,6), new Location(7,8), new Location(3,9),
			new Location(9,8), new Location(5,5), new Location(7,0));
	public static List<Location> pitLocsTest1 = Arrays.asList(new Location(4,1), new Location(7,4), new Location(7,6), new Location(7,7), new Location(7,8), new Location(7,9));
	
	public static List<Location> tokenLocsTest2 = Arrays.asList(new Location(1,2), new Location(1,3), new Location(1,5), new Location(5,5), new Location(5,4), new Location(1,6), new Location(7,8), new Location(3,9),
			new Location(9,8), new Location(5,5), new Location(7,0));
	public static List<Location> pitLocsTest2 = Arrays.asList(new Location(4,2), new Location(7,6), new Location(7,2), new Location(7,7), new Location(7,8), new Location(7,9));
	
	public static List<Location> tokenLocsTest3 = Arrays.asList(new Location(1,2), new Location(1,3), new Location(1,5), new Location(5,5), new Location(5,4), new Location(1,6), new Location(7,8), new Location(3,9),
			new Location(5,0), new Location(3,0), new Location(4,0));
	public static List<Location> pitLocsTest3 = Arrays.asList(new Location(4,2), new Location(7,6), new Location(7,3), new Location(6,7), new Location(6,8), new Location(6,9));
	
	public static List<List<Location>> allTokenLocs = Arrays.asList(tokenLocs1, tokenLocs2, tokenLocs3);
	public static List<List<Location>> allPitLocs = Arrays.asList(pitLocs1, pitLocs2, pitLocs3);
	
	public static List<List<Location>> allTokenLocsTest = Arrays.asList(tokenLocsTest1, tokenLocsTest2, tokenLocsTest3);
	public static List<List<Location>> allPitLocsTest = Arrays.asList(pitLocsTest1, pitLocsTest2, pitLocsTest3);
	
	public static final int NUM_TRAINING_SESSIONS = trainingGoalLocs.length;
	public static final int NUM_TESTING_SESSIONS = testingGoalLocs.length;

	//file names where results are stored
	public static String dataDir = "inputFiles\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	public static String participantDir = "C:\\ExperimentData_Dec2014\\";
	
	public static String simulationDir = "C:\\Extra\\";
	public static String rewardProceQName = simulationDir+"PQ_grid_changingTokensPits.csv";
	public static String rewardPerturbQName = simulationDir+"BQ_grid_changingTokensPits.csv";
	public static String rewardHRPerturbName = simulationDir+"BH_grid_changingTokensPits.csv";
	public static String rewardPRQLName = simulationDir+"PRQL_grid_changingTokensPits.csv";
	
	public static String qvaluesDir = simulationDir;
	
	public static String trainedQValuesDir = "C:\\Users\\julie\\Dropbox (MIT)\\trainingQValues\\";
}
