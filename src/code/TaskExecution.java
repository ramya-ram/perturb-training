package code;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

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
	
	public Color[] colorsTraining = {Color.BLUE, new Color(107, 142, 35), new Color(148,0,211)};
	public Color[] colorsTesting = {new Color(178,34,34), new Color(148,0,211), Color.BLUE, new Color(148,0,211)};
	
	public TaskExecution(GameView gameView, List<MyWorld> practiceWorlds, List<MyWorld> trainingWorlds, List<MyWorld> testingWorlds, ExperimentCondition condition){
		this.gameView = gameView;
		this.practiceWorlds = practiceWorlds;
		this.trainingWorlds = trainingWorlds;
		this.testingWorlds = testingWorlds;
		this.condition = condition;
		System.out.println(condition);
	}
	
	/**
	 * Run training and testing phases, according to which option is being run
	 */
	public void executeTask(){
		if(Main.CURRENT_EXECUTION == Main.SIMULATION){
			Pair<List<QValuesSet>, List<Policy>> trainedResult = runTrainingPhase();
			List<QValuesSet> trainedLearners = trainedResult.getFirst();
			List<Policy> trainedPolicies = trainedResult.getSecond();
			if(condition == ExperimentCondition.PRQL) {
				runTestingPhase(trainedLearners, trainedPolicies, -1);
				if(Main.SUB_EXECUTION == -1){ //Only run PRQL with different priors when SUB_EXECUTION == -1, not when SUB_EXECUTION == REWARD_OVER_ITERS 
					for(int i=0; i<Constants.NUM_TRAINING_SESSIONS; i++) {
						runTestingPhase(trainedLearners, trainedPolicies, i);
					}
				}
			} else {
				runTestingPhase(trainedLearners, trainedPolicies, -1);
			}
		}
		
		else if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN_TRAIN_TEST){
			runPracticeSession();
			Pair<List<QValuesSet>, List<Policy>> trainedResult = runTrainingPhase();
			List<QValuesSet> trainedLearners = trainedResult.getFirst();
			List<Policy> trainedPolicies = trainedResult.getSecond();
			runTestingPhase(trainedLearners, trainedPolicies, -1);
		}
		
		else if(Main.CURRENT_EXECUTION == Main.SIMULATION_HUMAN_TRAIN){
			runPracticeSession();
			Pair<List<QValuesSet>, List<Policy>> trainedResult = runTrainingPhase();
			List<QValuesSet> trainedLearners = trainedResult.getFirst();
			saveTrainingToFile(trainedLearners);
		}
		
		else if(Main.CURRENT_EXECUTION == Main.ROBOT_HUMAN_TEST){
			List<QValuesSet> trainedLearners = readTrainingFromFile();
			System.out.println("read from training files");
			Constants.MAX_TIME = 25;
			runTestingPhase(trainedLearners, null, -1);
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
			
		//practice session	
		QLearner practice1 = new QLearner(null, ExperimentCondition.PROCE_Q);
		QLearner practice2 = new QLearner(null, ExperimentCondition.PROCE_Q);
		
		try{
			practiceWorlds.get(0).setTitleLabel(1, null, -1);
			practice1.run(practiceWorlds.get(0), true, practiceWorlds.get(0).initialState(1));
			Constants.MAX_TIME = 10;
			practiceWorlds.get(1).setTitleLabel(2, null, -1);
			practice2.run(practiceWorlds.get(1), true, practiceWorlds.get(1).initialState(2));
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
		
		//first training session -- same for procedural and perturbation
		QLearner baseQLearner = new QLearner(null, ExperimentCondition.PROCE_Q);
		MyWorld trainWorld0 = trainingWorlds.get(0);
		trainWorld0.setTitleLabel(1, colorsTraining, 0);
		baseQLearner.run(trainWorld0, false /*withHuman*/);
		baseQLearner.run(trainWorld0, true, trainWorld0.initialState(1));
		trainWorld0.setTitleLabel(2, colorsTraining, 0);
		baseQLearner.run(trainWorld0, false);
		baseQLearner.run(trainWorld0, true, trainWorld0.initialState(2));
		learners.add(baseQLearner.currQValues);
		if(condition == ExperimentCondition.PRQL)
			policies.add(baseQLearner.computePolicy());
		
		if(condition == ExperimentCondition.ADAPT || condition == ExperimentCondition.PRQL){
			//perturbation training sessions
			for(int i=1; i<trainingWorlds.size(); i++){
				MyWorld trainWorld = trainingWorlds.get(i);
				QLearner perturbLearner = new QLearner(baseQLearner.currQValues, ExperimentCondition.ADAPT);
				trainWorld.setTitleLabel(1, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				perturbLearner.run(trainWorld, false);
				perturbLearner.run(trainWorld, true, trainWorld.initialState(i*2+1));
				trainWorld.setTitleLabel(2, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				perturbLearner.run(trainWorld, false);
				perturbLearner.run(trainWorld, true, trainWorld.initialState(i*2+2));
				learners.add(perturbLearner.currQValues);
				if(condition == ExperimentCondition.PRQL)
					policies.add(perturbLearner.computePolicy());
			}
		} else if(condition == ExperimentCondition.PERTURB_Q || condition == ExperimentCondition.PROCE_Q){ //both perturb and proce Q-learning use one qlearner to learn all training tasks
			//extra training sessions after base session
			for(int i=1; i<trainingWorlds.size(); i++){
				MyWorld trainWorld = trainingWorlds.get(i);
				trainWorld.setTitleLabel(1, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				baseQLearner.run(trainWorld, false);
				baseQLearner.run(trainWorld, true, trainWorld.initialState(i*2+1));
				trainWorld.setTitleLabel(2, colorsTraining, trainingWorlds.get(i).sessionNum-1);
				baseQLearner.run(trainWorld, false);
				baseQLearner.run(trainWorld, true, trainWorld.initialState(i*2+2));
			}
		}
		
		return new Pair<List<QValuesSet>, List<Policy>>(learners, policies);
	}
	
	/**
	 * Runs the testing phase
	 * Procedural uses Q-learning and is initialized with Q-values learned from training
	 * Perturbation uses Human-Robot Policy Reuse with the library learned from training
	 */
	public void runTestingPhase(List<QValuesSet> allLearners, List<Policy> allPolicies, int initialQValuesIndex){
		System.out.println("allLearners size "+allLearners.size()+" allPolicies size "+allPolicies.size());
		List<QValuesSet> learners = new ArrayList<QValuesSet>();
		if(condition == ExperimentCondition.ADAPT){
			learners.addAll(allLearners);
			System.out.println("Learners size "+learners.size());
			for(int i=0; i<testingWorlds.size(); i++){
				MyWorld testWorld = testingWorlds.get(i);
				AdaPTLearner perturbLearner = new AdaPTLearner(testWorld, learners);
				testWorld.setTitleLabel(1, colorsTesting, testWorld.sessionNum-1);
				perturbLearner.runHRPerturb(false);
				perturbLearner.runHRPerturb(true, testWorld.initialState(testWorld.sessionNum));
			}
		} else if(condition == ExperimentCondition.PRQL){
			if(initialQValuesIndex >= 0)
				learners.add(allLearners.get(initialQValuesIndex));
			else
				learners.add(new QValuesSet());
			System.out.println("Library size "+allPolicies.size()+" Learners size "+learners.size());
			for(int i=0; i<testingWorlds.size(); i++){
				MyWorld testWorld = testingWorlds.get(i);
				PRQLearner learner = new PRQLearner(testWorld, allPolicies, learners.get(0));
				testWorld.setTitleLabel(1, colorsTesting, testWorld.sessionNum-1);
				learner.runPRQL(false);
				learner.runPRQL(true, testWorld.initialState(testWorld.sessionNum));
			}
		} else if(condition == ExperimentCondition.Q_LEARNING){
			for(int i=0; i<testingWorlds.size(); i++){
				MyWorld testWorld = testingWorlds.get(i);
				QLearner learner = new QLearner(new QValuesSet(), ExperimentCondition.Q_LEARNING);
				learner.run(testWorld, false);
				learner.run(testWorld, true, testWorld.initialState(i*2+1));
			}
		} else {
			//Q-learning proce and perturb testing sessions
			for(MyWorld testWorld : testingWorlds){
				QLearner testQLearner = new QLearner(allLearners.get(0), condition); //both proce and perturb Q only have one Q-value function so it is directly transferred to the test case
				testWorld.setTitleLabel(1, colorsTesting, testWorld.sessionNum-1);
				testQLearner.run(testWorld, false);
				testQLearner.run(testWorld, true, testWorld.initialState(testWorld.sessionNum));
			}
		}
	}
}
