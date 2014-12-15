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
		QLearner baseQLearner = new QLearner(null, true, ExperimentCondition.PROCE_Q);
		setTitleLabel(trainingWorlds.get(0), colorsTraining[0]);
		baseQLearner.run(trainingWorlds.get(0), false /*withHuman*/, false /*computePolicy*/);
		baseQLearner.run(trainingWorlds.get(0), true, false);
		baseQLearner.run(trainingWorlds.get(0), false, false);
		Policy basePolicy = baseQLearner.run(trainingWorlds.get(0), true, true);
		//TODO: possibly get policy from training session 1 for the library
		learners.add(baseQLearner);
		library.add(basePolicy);
		baseQLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), condition+"_"+0, Constants.print);
		
		if(condition == ExperimentCondition.HRPR){
			//perturbation training sessions
			for(int i=1; i<trainingWorlds.size(); i++){
				QLearner perturbLearner = new QLearner(new QValuesSet(baseQLearner.robotQValues, baseQLearner.jointQValues), false, ExperimentCondition.HRPR);
				setTitleLabel(trainingWorlds.get(i), colorsTraining[trainingWorlds.get(i).sessionNum-1]);
				perturbLearner.run(trainingWorlds.get(i), false, false);
				perturbLearner.run(trainingWorlds.get(i), true, false);
				perturbLearner.run(trainingWorlds.get(i), false, false);
				Policy policy = perturbLearner.run(trainingWorlds.get(i), true, true);
				library.add(policy);
				learners.add(perturbLearner);
				perturbLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), condition+"_"+i, Constants.print);
			}
		} else { //both perturb and proce Q-learning use one qlearner to learn all training tasks
			//extra training sessions after base session
			for(int i=1; i<trainingWorlds.size(); i++){
				System.out.println("trainworld "+trainingWorlds.get(i).sessionNum);
				setTitleLabel(trainingWorlds.get(i), colorsTraining[trainingWorlds.get(i).sessionNum-1]);
				baseQLearner.run(trainingWorlds.get(i), false, false);
				baseQLearner.run(trainingWorlds.get(i), true, false);
				baseQLearner.run(trainingWorlds.get(i), false, false);
				baseQLearner.run(trainingWorlds.get(i), true, false);
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
				PolicyReuseLearner PRLearner = new PolicyReuseLearner(testWorld, library,
						new QValuesSet(trainedLearners.get(0).robotQValues, trainedLearners.get(0).jointQValues), null);
				setTitleLabel(testWorld, colorsTesting[testWorld.sessionNum-1]);
				PRLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testbefore_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
				PRLearner.policyReuse(false, false);
				PRLearner.policyReuse(true, false);
				PRLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testafter_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
			}
		} else {
			//Q-learning proce and perturb testing sessions
			for(MyWorld testWorld : testingWorlds){
				QLearner testQLearner = new QLearner(new QValuesSet(
						trainedLearners.get(0).robotQValues, trainedLearners.get(0).jointQValues), false, condition);
				setTitleLabel(testWorld, colorsTesting[testWorld.sessionNum-1]);
				testQLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "testbefore_"+condition+"_"+(testWorld.sessionNum-1), Constants.print);
				testQLearner.run(testWorld, false, false);
				testQLearner.run(testWorld, true, false);
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
}
