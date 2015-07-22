package code;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import PR2_robot.GameView;

/**
 * Executes the training and testing phase using the given worlds
 * Appropriately runs procedural or perturbation depending on the boolean perturb parameter
 */
public class TaskExecution {
	public GameView gameView;
	public List<MyWorld> practiceWorlds;
	public List<MyWorld> trainingWorlds;
	public List<MyWorld> testingWorlds;
	public ExperimentCondition condition;
	public MatlabProxyFactory factory;
	public MatlabProxy proxy;
	
	public Color[] colorsTraining = {Color.BLUE, new Color(107, 142, 35), new Color(148,0,211)};
	public Color[] colorsTesting = {new Color(178,34,34), new Color(148,0,211), Color.BLUE, new Color(148,0,211)};
	
	public TaskExecution(GameView gameView, List<MyWorld> practiceWorlds, List<MyWorld> trainingWorlds, List<MyWorld> testingWorlds, ExperimentCondition condition){
		this.gameView = gameView;
		this.practiceWorlds = practiceWorlds;
		this.trainingWorlds = trainingWorlds;
		this.testingWorlds = testingWorlds;
		this.condition = condition;
		System.out.println(condition);
		
		try{
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();
	
			//Create a proxy, which we will use to control MATLAB
		    factory = new MatlabProxyFactory(options);
		    proxy = factory.getProxy();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public int[] populateArrayWithValue(int arrayLength, int value){
		int[] array = new int[arrayLength];
		for(int j=0; j<array.length; j++)
			array[j] = value;
		return array;
	}
	
	/**
	 * Run training and testing phases, according to which option is being run
	 */
	public void executeTask(){
		int[] initialQValuesIndices = new int[Constants.NUM_TESTING_SESSIONS];
		for(int i=0; i<initialQValuesIndices.length; i++)
			initialQValuesIndices[i] = -1;
		
		if(Main.CURRENT_EXECUTION == Main.SIMULATION){
			Pair<List<QValuesSet>, List<Policy>> trainedResult = runTrainingPhase();
			List<QValuesSet> trainedLearners = trainedResult.getFirst();
			List<Policy> trainedPolicies = trainedResult.getSecond();
			if(condition == ExperimentCondition.PRQL) {
				runTestingPhase(trainedLearners, trainedPolicies, initialQValuesIndices); //runs PRQL with an uninformative prior (starts with value function initialized with all zeros)
				if(Main.SUB_EXECUTION == Main.REWARD_LIMITED_TIME){ //only run PRQL with different priors when SUB_EXECUTION == REWARD_LIMITED_TIME
					for(int i=0; i<Constants.NUM_TRAINING_SESSIONS; i++) {
						initialQValuesIndices = populateArrayWithValue(Constants.NUM_TESTING_SESSIONS, i); //all testing tasks will begin with the values learned from training task i
						runTestingPhase(trainedLearners, trainedPolicies, initialQValuesIndices); //runs PRQL with a prior initialized with the value function learned from each training task
					}
				}
			} else if (condition == ExperimentCondition.PRQL_RBM) {
				//run matlab function runRBM to get closest MDP to each test task
				try {
				  	String addPath = "addpath('"+Paths.get("").toAbsolutePath().toString()+"\\RBM_MatlabCode')";
				  	proxy.eval(addPath);
				  	
				    for(int i=1; i<=Constants.NUM_TESTING_SESSIONS; i++){
				    	//initialQValuesIndices[i] = 
				    	proxy.feval("runRBM", 3, 100, 5, i);
				    	/*for(Object o : result)
				    		System.out.println(o);
				    	Object result0 = result[0];
				    	System.out.println(result0);
				    	int intresult = (int)result0;
				    	System.out.println(intresult);*/
				    }
				  	String removePath = "rmpath('"+Paths.get("").toAbsolutePath().toString()+"\\RBM_MatlabCode')";
				  	proxy.eval(removePath);
				  	
				    //Disconnect the proxy from MATLAB
				    proxy.disconnect();
				    proxy = factory.getProxy();
				    
				} catch(Exception e){
					e.printStackTrace();
				}
			    runTestingPhase(trainedLearners, trainedPolicies, initialQValuesIndices); //starts with value function initialized with the closest MDP/task from training (as determined using the RBM)
			} else {
				runTestingPhase(trainedLearners, trainedPolicies, initialQValuesIndices); //for AdaPT, no prior is needed
			}
		}
		
		//runs simulated training and simulated testing for human subject experiments
		else if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN_TRAIN_TEST){
			runPracticeSession();
			Pair<List<QValuesSet>, List<Policy>> trainedResult = runTrainingPhase();
			List<QValuesSet> trainedLearners = trainedResult.getFirst();
			List<Policy> trainedPolicies = trainedResult.getSecond();
			runTestingPhase(trainedLearners, trainedPolicies, initialQValuesIndices);
		}
		
		//runs simulated training human subject experiments
		else if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN_TRAIN){
			runPracticeSession();
			Pair<List<QValuesSet>, List<Policy>> trainedResult = runTrainingPhase();
			List<QValuesSet> trainedLearners = trainedResult.getFirst();
			saveTrainingToFile(trainedLearners);
		}
		
		//runs embodied robot testing for human subject experiments, using learned value functions from simulation training
		else if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN_TEST){
			List<QValuesSet> trainedLearners = readTrainingFromFile();
			System.out.println("read from training files");
			Constants.MAX_TIME = 25;
			runTestingPhase(trainedLearners, null, initialQValuesIndices);
		}
	}
	
	/**
	 * If only the training is in simulation (and the testing is with the PR2), the training Q-values are saved to a file, which can be read when starting the testing sessions with the robot
	 */
	public void saveTrainingToFile(List<QValuesSet> learners){
		File dir = new File(Constants.trainedQValuesDir);
		for(File file : dir.listFiles())
			file.delete();
		for(int i=0; i<learners.size(); i++){
			QValuesSet set = learners.get(i);
			saveQValuesToFile(set, i);
		}
	}
	
	/**
	 * Writes the Q-values to a file
	 */
	public void saveQValuesToFile(QValuesSet learner, int index) {
		try {
			BufferedWriter robotWriter = new BufferedWriter(new FileWriter(new File(Constants.trainedQValuesDir+"robot"+index+".txt")));
			BufferedWriter jointWriter = new BufferedWriter(new FileWriter(new File(Constants.trainedQValuesDir+"joint"+index+".txt")));
			for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);													
				for(Action robotAction : Action.values()){
					robotWriter.write(learner.robotQValues[state.getId()][robotAction.ordinal()]+",");
					for(Action humanAction : Action.values()){
						jointWriter.write(learner.jointQValues[state.getId()][robotAction.ordinal()][humanAction.ordinal()]+",");
					}
				}
			}
			robotWriter.close();
			jointWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * When working with the PR2 during testing, the Q-values learned from training with the simulated robot is read
	 */
	public List<QValuesSet> readTrainingFromFile(){
		List<QValuesSet> trainedQValues = new ArrayList<QValuesSet>();
		try {
			for(int i=0; i<3; i++){
				trainedQValues.add(readQValuesFromFile(i));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return trainedQValues;
	}
	
	/**
	 * Reads saved Q-values from a file
	 */
	public QValuesSet readQValuesFromFile(int index){
		QValuesSet set = new QValuesSet();
		try{
			BufferedReader jointReader = new BufferedReader(new FileReader(new File(Constants.trainedQValuesDir+"joint"+index+".txt")));
			String[] jointValues = jointReader.readLine().split(",");
			
			BufferedReader robotReader = new BufferedReader(new FileReader(new File(Constants.trainedQValuesDir+"robot"+index+".txt")));
			String[] robotValues = robotReader.readLine().split(",");
			
			System.out.println("joint size "+jointValues.length);
			System.out.println("robot size "+robotValues.length);
	
			jointReader.close();
			robotReader.close();
			
			int jointNum=0;
			int robotNum=0;
			for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);													
				for(Action robotAction : Action.values()){
					double robotValue = Double.parseDouble(robotValues[robotNum]);
					if(robotValue != 0){
						set.robotQValues[state.getId()][robotAction.ordinal()] = robotValue;	
					}
					robotNum++;
					for(Action humanAction : Action.values()){
						double jointValue = Double.parseDouble(jointValues[jointNum]);
						if(jointValue != 0){
							set.jointQValues[state.getId()][humanAction.ordinal()][robotAction.ordinal()] = jointValue;
						}
						jointNum++;
					}
				}
			} 
		} catch(Exception e){
			e.printStackTrace();
		}
		return set;
	}
	
	/**
	 * Allows the human to practice using the simulated interface for two simple sessions before beginning training
	 */
	public void runPracticeSession(){
		Main.saveToFile = false;
			
		//participants do two practice sessions before beginning the training tasks
		QLearner practice1 = new QLearner(null, condition);
		QLearner practice2 = new QLearner(null, condition);
		
		try{
			practiceWorlds.get(0).setTitleLabel(1, null, -1);
			practice1.runQLearning(practiceWorlds.get(0), true, practiceWorlds.get(0).initialState(1));
			Constants.MAX_TIME = 10;
			practiceWorlds.get(1).setTitleLabel(2, null, -1);
			practice2.runQLearning(practiceWorlds.get(1), true, practiceWorlds.get(1).initialState(2));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Run all training sessions
	 * Regardless of the training type, the first session runs through the base task
	 * The second and third sessions either have perturbations or are repeated rounds of the base task
	 */
	public Pair<List<QValuesSet>, List<Policy>> runTrainingPhase(){
		Main.saveToFile = true;
		List<QValuesSet> learners = new ArrayList<QValuesSet>();
		List<Policy> policies = new ArrayList<Policy>();
		
		//for each task, the robot works with the person twice, simulating before each of these two interactions
		//first training session -- same for procedural and perturbation
		QLearner baseQLearner = new QLearner(null, condition);
		MyWorld trainWorld0 = trainingWorlds.get(0);
		trainWorld0.setTitleLabel(1, colorsTraining, 0);
		baseQLearner.runQLearning(trainWorld0, false /*withHuman*/); //robot simulates on the task
		baseQLearner.runQLearning(trainWorld0, true, trainWorld0.initialState(1)); //robot works with the person
		trainWorld0.setTitleLabel(2, colorsTraining, 0);
		baseQLearner.runQLearning(trainWorld0, false); //robot simulates on the task
		baseQLearner.runQLearning(trainWorld0, true, trainWorld0.initialState(2)); //robot works with the person
		learners.add(baseQLearner.currQValues); //learned Q-value function is saved
		if(condition == ExperimentCondition.PRQL || condition == ExperimentCondition.PRQL_RBM)
			policies.add(baseQLearner.computePolicy()); //learned policy (saves only optimal action for each state, not all Q-values of all actions) is saved
		
		if(condition == ExperimentCondition.ADAPT || condition == ExperimentCondition.PRQL || condition == ExperimentCondition.PRQL_RBM){
			//perturbation training sessions
			for(int i=1; i<trainingWorlds.size(); i++){
				MyWorld trainWorld = trainingWorlds.get(i);
				QLearner perturbLearner = new QLearner(baseQLearner.currQValues, condition);
				trainWorld.setTitleLabel(1, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				perturbLearner.runQLearning(trainWorld, false); //robot simulates on the task
				perturbLearner.runQLearning(trainWorld, true, trainWorld.initialState(i*2+1)); //robot works with the person
				trainWorld.setTitleLabel(2, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				perturbLearner.runQLearning(trainWorld, false); //robot simulates on the task
				perturbLearner.runQLearning(trainWorld, true, trainWorld.initialState(i*2+2)); //robot works with the person
				learners.add(perturbLearner.currQValues); //learned Q-value function is saved
				if(condition == ExperimentCondition.PRQL || condition == ExperimentCondition.PRQL_RBM)
					policies.add(perturbLearner.computePolicy()); //learned policy (saves only optimal action for each state, not all Q-values of all actions) is saved
			}
		} else if(condition == ExperimentCondition.PERTURB_Q || condition == ExperimentCondition.PROCE_Q){
			//both procedural and perturbation Q-learning use one Q-learner to learn all training tasks
			for(int i=1; i<trainingWorlds.size(); i++){
				MyWorld trainWorld = trainingWorlds.get(i);
				trainWorld.setTitleLabel(1, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				baseQLearner.runQLearning(trainWorld, false); //robot simulates on the task
				baseQLearner.runQLearning(trainWorld, true, trainWorld.initialState(i*2+1)); //robot works with the person
				trainWorld.setTitleLabel(2, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				baseQLearner.runQLearning(trainWorld, false); //robot simulates on the task
				baseQLearner.runQLearning(trainWorld, true, trainWorld.initialState(i*2+2)); //robot works with the person
			}
		}
		
		return new Pair<List<QValuesSet>, List<Policy>>(learners, policies);
	}
	
	/**
	 * Runs the testing phase
	 */
	public void runTestingPhase(List<QValuesSet> allLearners, List<Policy> allPolicies, int[] initialQValuesIndices){
		for(int value : initialQValuesIndices)
			System.out.print(value+" ");
		System.out.println();
		System.out.println("AllLearners size "+allLearners.size()+" AllPolicies size "+allPolicies.size());
		if(condition == ExperimentCondition.ADAPT){
			//AdaPT uses all value functions learned from training and adapts them for the new task so learners includes all learners from training
			System.out.println("Learners size "+allLearners.size());
			for(int i=0; i<testingWorlds.size(); i++){
				MyWorld testWorld = testingWorlds.get(i);
				AdaPTLearner learner = new AdaPTLearner(testWorld, allLearners, condition);
				testWorld.setTitleLabel(1, colorsTesting, testWorld.sessionNum-1);
				learner.runAdaPT(false); //robot simulates on the task 
				learner.runAdaPT(true, testWorld.initialState(testWorld.sessionNum)); //robot works with the person
			}
		} else if(condition == ExperimentCondition.PRQL || condition == ExperimentCondition.PRQL_RBM){
			//System.out.println("Library size "+allPolicies.size()+" Learners size "+learners.size());
			for(int i=0; i<testingWorlds.size(); i++){
				MyWorld testWorld = testingWorlds.get(i);
				QValuesSet initialQValues = getQValues(allLearners, initialQValuesIndices[i]);
				PRQLearner learner = new PRQLearner(testWorld, allPolicies, initialQValues, condition);
				testWorld.setTitleLabel(1, colorsTesting, testWorld.sessionNum-1);
				learner.runPRQL(false); //robot simulates on the task
				learner.runPRQL(true, testWorld.initialState(testWorld.sessionNum)); //robot works with the person
			}
		} else if(condition == ExperimentCondition.Q_LEARNING){
			for(int i=0; i<testingWorlds.size(); i++){
				MyWorld testWorld = testingWorlds.get(i);
				//Q-learning from scratch starts with a value function initialized with all zeros (uninformative prior)
				QLearner learner = new QLearner(new QValuesSet(), condition);
				learner.runQLearning(testWorld, false); //robot simulates on the task
				learner.runQLearning(testWorld, true, testWorld.initialState(i*2+1)); //robot works with the person
			}
		} else {
			//Q-learning procedural and perturbation testing sessions
			for(MyWorld testWorld : testingWorlds){
				//both procedural and perturbation Q-learning have only one Q-value function, this is directly transferred to the test case
				QLearner testQLearner = new QLearner(allLearners.get(0), condition);
				testWorld.setTitleLabel(1, colorsTesting, testWorld.sessionNum-1);
				testQLearner.runQLearning(testWorld, false); //robot simulates on the task
				testQLearner.runQLearning(testWorld, true, testWorld.initialState(testWorld.sessionNum)); //robot works with the person
			}
		}
	}
	
	public QValuesSet getQValues(List<QValuesSet> allLearners, int qValuesIndex){
		if(qValuesIndex >= 0) //if using a previously learned value function as a prior, PRQL will begin with those values
			return allLearners.get(qValuesIndex);
		return new QValuesSet(); //if using no prior, PRQL will begin with a value function of all zeros
	}
}
