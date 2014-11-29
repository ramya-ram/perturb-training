package code;

public class Constants {
	public static final double GAMMA = 1; // gamma is penalty on delayed result
	public static final double ALPHA = 0.05; // learning rate
	public static final double TEMP = 0.5; //temperature parameter 
	public static final double DELTA_TEMP = 0.1; //change in temperature parameter 
	public static final double PAST_PROB = 1; //probability of choosing a past policy 
	public static final double DECAY_VALUE = 0.95; //decay the probability of choosing past policies
	
	public static int MAX_TIME = 15;
	public static final double SIMULATION_EPSILON = 0.1; // epsilon used in picking an action/how much should be explore
	public static final double HUMAN_EPSILON = 0; // epsilon used in picking an action/how much should be explore
	public static final double EPSILON = 0.1;
	
	//num of times to run
	public static final int NUM_EPISODES = 200000;
	public static final int NUM_EPISODES_TEST = 1000;
	public static final int NUM_STEPS_PER_EPISODE = 20;
	
	public static final int NUM_TRAINING_SESSIONS = 3;
	public static final int NUM_TESTING_SESSIONS = 3;
	
	public static boolean predefined = false;
	public static boolean print = false;
	public static int NUM_AVERAGING = 20;
	
	//file names where results are stored
	public static String dataDir = "C:\\ExperimentData_Dec2014\\";
	public static String predefinedPerturb2FileName = dataDir+"predefinedPerturb2.csv";
	public static String predefinedPerturb1FileName = dataDir+"predefinedPerturb1.csv";
	public static String predefinedPerturb0FileName = dataDir+"predefinedPerturb0.csv";
	public static String predefinedProceFileName = dataDir+"predefinedProce.csv";
	public static String jointQValuesFile = dataDir+"jointQValuesOffline.csv";
	public static String robotQValuesFile = dataDir+"robotQValuesOffline.csv";
	
	public static String participantDir = dataDir;
	
	public static String simulationDir = "C:\\RSS_SimulationResults\\";
	public static String rewardProceName = simulationDir+"testproceReward_39_93_46_05Train_EntireStates_test11033_1000iter.csv";
	public static String rewardPerturbName = simulationDir+"testperturbReward_39_93_46_05Train_EntireStates_test11033_1000iter.csv";

	public static String fileBase = Constants.participantDir;
	public static String rewardName = fileBase+"Reward.csv";
	public static String iterName = fileBase+"Iter.csv";
	public static String rewardHumanName = fileBase+"RewardHuman.csv";
	public static String iterHumanName = fileBase+"IterHuman.csv";
	public static String timeName = fileBase+"Time.csv";
	public static String socketTestOutputName = fileBase+"SocketTestOutput.txt";
	public static String robotUpdatesName = fileBase+"robotUpdates.csv";
	public static String robotSuggName = fileBase+"robotSuggestions.csv";
	public static String humanUpdatesName = fileBase+"humanUpdates.csv";
	public static String humanSuggName = fileBase+"humanSuggestions.csv";
	public static String episodeName = fileBase+"Episode.csv";
	
	public static String humanAccName = fileBase+"humanAccepts.csv";
	public static String robotAccName = fileBase+"robotAccepts.csv";
	public static String humanRejName = fileBase+"humanRejects.csv";
	public static String robotRejName = fileBase+"robotRejects.csv";
}
