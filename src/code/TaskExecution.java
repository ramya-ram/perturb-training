package code;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import PR2_robot.GameView;

/**
 * Executes the training and testing phase using the given worlds
 * Appropriately runs procedural or perturbation depending on the boolean perturb parameter
 */
public class TaskExecution {
	public GameView gameView;
	public List<MyWorld> trainingWorlds;
	public List<MyWorld> testingWorlds;
	public ExperimentCondition condition;
	
	public Color[] colorsTraining = {Color.GREEN, Color.CYAN, Color.MAGENTA};
	public Color[] colorsTesting = {Color.ORANGE, Color.RED, Color.GREEN};
	
	public TaskExecution(GameView gameView, List<MyWorld> trainingWorlds, List<MyWorld> testingWorlds, ExperimentCondition condition){
		this.gameView = gameView;
		this.trainingWorlds = trainingWorlds;
		this.testingWorlds = testingWorlds;
		this.condition = condition;
	}
	
	/**
	 * Run training and testing phases
	 */
	public void executeTask(){		
		System.out.println("EXECUTE TASK");
		
		if(Main.CURRENT_EXECUTION != Main.SIMULATION)
			runPracticeSession();

		Pair<List<QLearner>, PolicyLibrary> trainedResult = runTrainingPhase();
		runTestingPhase(trainedResult.getFirst(), trainedResult.getSecond());
	}
	
	public void runPracticeSession(){
		/*Main.saveToFile = false;
		MyWorld practiceWorld1 = new MyWorld(Constants.TRAINING, false, 0);
		MyWorld practiceWorld2 = new MyWorld(Constants.TRAINING, false, 0);
		
		//practice session	
		QLearner practice1 = new QLearner(null, true, ExperimentCondition.PROCE_Q);
		QLearner practice2 = new QLearner(null, true, ExperimentCondition.PROCE_Q);
		
		try{
			
		} catch(Exception e){
			e.printStackTrace();
		}*/
	}

	
	/**
	 * Run all training sessions
	 * Regardless of the training type, the first session runs through the base task
	 * The second and third sessions either have perturbations or are repeated rounds of the base task
	 */
	public Pair<List<QLearner>, PolicyLibrary> runTrainingPhase(){
		Main.saveToFile = true;
		List<QLearner> learners = new ArrayList<QLearner>();
		PolicyLibrary library = new PolicyLibrary();
		
		//first training session -- same for procedural and perturbation
		QLearner baseQLearner = new QLearner(null, ExperimentCondition.PROCE_Q);
		MyWorld trainWorld0 = trainingWorlds.get(0);
		setTitleLabel(trainWorld0, colorsTraining[0]);
		baseQLearner.run(trainWorld0, false /*withHuman*/, false /*computePolicy*/);
		baseQLearner.run(trainWorld0, true, false, initialState(trainWorld0, 1));
		baseQLearner.run(trainWorld0, false, false);
		/*Policy basePolicy = */baseQLearner.run(trainWorld0, true, true, initialState(trainWorld0, 2));
		//TODO: possibly get policy from training session 1 for the library
		learners.add(baseQLearner);
		//library.add(basePolicy);
		baseQLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), condition+"_"+0, Constants.print);
		
		if(condition == ExperimentCondition.HRPR){
			//perturbation training sessions
			for(int i=1; i<trainingWorlds.size(); i++){
				MyWorld trainWorld = trainingWorlds.get(i);
				QLearner perturbLearner = new QLearner(baseQLearner.currQValues, ExperimentCondition.HRPR);
				setTitleLabel(trainWorld, colorsTraining[trainingWorlds.get(i).sessionNum-1]);
				perturbLearner.run(trainWorld, false, false);
				perturbLearner.run(trainWorld, true, false, initialState(trainWorld, i*2+1));
				perturbLearner.run(trainWorld, false, false);
				Policy policy = perturbLearner.run(trainWorld, true, true, initialState(trainWorld, i*2+2));
				library.add(policy);
				learners.add(perturbLearner);
				perturbLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), condition+"_"+i, Constants.print);
			}
		} else { //both perturb and proce Q-learning use one qlearner to learn all training tasks
			//extra training sessions after base session
			for(int i=1; i<trainingWorlds.size(); i++){
				MyWorld trainWorld = trainingWorlds.get(i);
				System.out.println("trainworld "+trainingWorlds.get(i).sessionNum);
				setTitleLabel(trainWorld, colorsTraining[trainingWorlds.get(i).sessionNum-1]);
				baseQLearner.run(trainWorld, false, false);
				baseQLearner.run(trainWorld, true, false, initialState(trainWorld, i*2+1));
				baseQLearner.run(trainWorld, false, false);
				baseQLearner.run(trainWorld, true, false, initialState(trainWorld, i*2+2));
				baseQLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), condition+"_"+i, Constants.print);
			}
		}
		
		return new Pair<List<QLearner>, PolicyLibrary>(learners, library);
	}
	
	/**
	 * Runs the testing phase
	 * Procedural uses Q-learning and is initialized with Q-values learned from training
	 * Perturbation uses Human-Robot Policy Reuse with the library learned from training
	 */
	public void runTestingPhase(List<QLearner> trainedLearners, PolicyLibrary library){
		if(condition == ExperimentCondition.HRPR){
			for(MyWorld testWorld : testingWorlds){
				PolicyReuseLearner PRLearner = new PolicyReuseLearner(testWorld, trainedLearners);
				setTitleLabel(testWorld, colorsTesting[testWorld.sessionNum-1]);
				PRLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testbefore_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
				PRLearner.policyReuse(false, false);
				PRLearner.policyReuse(true, false, initialState(testWorld, testWorld.sessionNum));
				PRLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testafter_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
			}
		} else {
			//Q-learning proce and perturb testing sessions
			for(MyWorld testWorld : testingWorlds){
				QLearner testQLearner = new QLearner(trainedLearners.get(0).currQValues, condition);
				setTitleLabel(testWorld, colorsTesting[testWorld.sessionNum-1]);
				testQLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testbefore_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
				testQLearner.run(testWorld, false, false);
				testQLearner.run(testWorld, true, false, initialState(testWorld, testWorld.sessionNum));
				testQLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testafter_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
			}
		}
	}
	
	public void setTitleLabel(MyWorld world, Color color){
		String str = "";
		if(world.typeOfWorld == Constants.TRAINING)
			str+= "Training Session ";
		else
			str+= "Testing Session ";
		str += world.sessionNum+" -- Observation: Wind = "+world.simulationWind+" Dryness= "+world.simulationDryness;
		if(gameView != null)
			gameView.setTitleLabel(str);
	}
	
	public State initialState(MyWorld myWorld, int roundNum){
		if(myWorld.typeOfWorld == Constants.PRACTICE){
			if(roundNum == 1){
				int[] stateOfFires = {3,3,3,3,3};
				return new State(stateOfFires);
			} else if(roundNum == 2){
				int[] stateOfFires = {3,2,0,3,1};
				return new State(stateOfFires);
			}	
		} else if(myWorld.typeOfWorld == Constants.TRAINING){
			if(myWorld.perturb){
				switch(roundNum){
					case 1:
						int[] stateOfFires = {2,3,3,1,2};
						return new State(stateOfFires);
					case 2:
						int[] stateOfFires1 = {2,2,1,3,3};
						return new State(stateOfFires1);
					case 3:
						int[] stateOfFires2 = {0,2,1,2,3};
						return new State(stateOfFires2);
					case 4:
						int[] stateOfFires3 = {0,1,1,2,3};
						return new State(stateOfFires3);
					case 5:
						int[] stateOfFires4 = {3,3,3,2,3};
						return new State(stateOfFires4);
					case 6:
						int[] stateOfFires5 = {3,3,2,3,1};
						return new State(stateOfFires5);
				}
			} else if(!myWorld.perturb) {
				switch(roundNum){
					case 1:
						int[] stateOfFires = {2,3,3,1,2};
						return new State(stateOfFires);
					case 2:
						int[] stateOfFires1 = {2,2,1,3,3};
						return new State(stateOfFires1);
					case 3:
						int[] stateOfFires2 = {3,1,3,1,2};
						return new State(stateOfFires2);
					case 4:
						int[] stateOfFires3 = {2,3,1,1,3};
						return new State(stateOfFires3);
					case 5:
						int[] stateOfFires4 = {3,3,2,3,3};
						return new State(stateOfFires4);
					case 6:
						int[] stateOfFires5 = {3,2,3,3,3};
						return new State(stateOfFires5);
				}
			}
		} else if(myWorld.typeOfWorld == Constants.TESTING) {
			if(roundNum == 1){
				int[] stateOfFires = {3,1,3,1,1};
				return new State(stateOfFires);
			} else if(roundNum == 2){
				int[] stateOfFires = {1,0,3,3,1};
				return new State(stateOfFires);
			} else if(roundNum == 3){
				int[] stateOfFires = {0,1,1,1,3};
				return new State(stateOfFires);
			} 
		}
		return null;
	}
}
