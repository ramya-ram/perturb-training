package code;

import java.util.ArrayList;
import java.util.List;

public class TaskExecution {
	List<MyWorld> trainingWorlds;
	boolean perturb;
	public TaskExecution(List<MyWorld> trainingWorlds, List<MyWorld> testingWorlds, boolean perturb){
		this.trainingWorlds = trainingWorlds;
		this.perturb = perturb;
	}
	
	public void executeTask(){
		Pair<List<QLearner>, PolicyLibrary> trainedResult = runTrainingPhase();
		runTestingPhase(trainedResult.getFirst(), trainedResult.getSecond());
	}
	
	public Pair<List<QLearner>, PolicyLibrary> runTrainingPhase(){
		List<QLearner> learners = new ArrayList<QLearner>();
		PolicyLibrary library = new PolicyLibrary();
		
		//first training session -- same for procedural and perturbation
		QLearner baseQLearner = new QLearner(Main.connect, null, true);
		baseQLearner.run(trainingWorlds.get(0), false /*withHuman*/, false /*computePolicy*/);
		baseQLearner.run(trainingWorlds.get(0), false, true);
		learners.add(baseQLearner);
		
		if(perturb){
			//perturbation training sessions
			for(int i=1; i<trainingWorlds.size(); i++){
				QLearner perturbLearner = new QLearner(Main.connect, 
						new QValuesSet(baseQLearner.robotQValues, baseQLearner.jointQValues), false);
				perturbLearner.run(trainingWorlds.get(i), false, false);
				Policy policy = perturbLearner.run(trainingWorlds.get(i), false, true);
				library.add(policy);
				learners.add(perturbLearner);
			}
		} else {
			//procedural extra training sessions
			for(int i=1; i<trainingWorlds.size(); i++){
				baseQLearner.run(trainingWorlds.get(i), false /*withHuman*/, false /*computePolicy*/);
				baseQLearner.run(trainingWorlds.get(i), false, false);
			}
		}
		
		return new Pair<List<QLearner>, PolicyLibrary>(learners, library);
	}
	
	public void runTestingPhase(List<QLearner> trainedLearners, PolicyLibrary library){
		
	}
}
