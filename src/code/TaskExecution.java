package code;

import java.util.ArrayList;
import java.util.List;

public class TaskExecution {
	List<MyWorld> trainingWorlds;
	List<MyWorld> testingWorlds;
	boolean perturb;
	
	//prior probabilities for environment variables = wind, dryness
	public double[] probWind;
	public double[] probDryness;
	public double[][] probObsGivenWind;
	public double[][] probObsGivenDryness;
	
	public TaskExecution(List<MyWorld> trainingWorlds, List<MyWorld> testingWorlds, boolean perturb){
		this.trainingWorlds = trainingWorlds;
		this.testingWorlds = testingWorlds;
		this.perturb = perturb;
	}
	
	public void executeTask(){
		initPriorProbabilities();

		Pair<List<QLearner>, PolicyLibrary> trainedResult = runTrainingPhase();
		runTestingPhase(trainedResult.getFirst(), trainedResult.getSecond());
	}
	
	public Pair<List<QLearner>, PolicyLibrary> runTrainingPhase(){
		List<QLearner> learners = new ArrayList<QLearner>();
		PolicyLibrary library = new PolicyLibrary();
		
		//first training session -- same for procedural and perturbation
		QLearner baseQLearner = new QLearner(Main.connect, null, true);
		baseQLearner.run(trainingWorlds.get(0), false /*withHuman*/, false /*computePolicy*/);
		baseQLearner.run(trainingWorlds.get(0), false, false);
		//TODO: possibly get policy from training session 1 for the library
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
				baseQLearner.run(trainingWorlds.get(i), false, false);
				baseQLearner.run(trainingWorlds.get(i), false, false);
			}
		}
		
		return new Pair<List<QLearner>, PolicyLibrary>(learners, library);
	}
	
	public void runTestingPhase(List<QLearner> trainedLearners, PolicyLibrary library){
		if(perturb){
			for(MyWorld testWorld : testingWorlds){
				calculateTestSimulationWindDryness(testWorld);

				double[] priorProbs = calculatePrior(trainingWorlds, testWorld);
				int maxPolicy = Tools.calculateMax(priorProbs);
				PolicyReuseLearner PRLearner = new PolicyReuseLearner(testWorld, Main.connect, library,
						new QValuesSet(trainedLearners.get(maxPolicy).robotQValues, trainedLearners.get(maxPolicy).jointQValues), priorProbs);
				PRLearner.numOfNonZeroQValues(new State(new int[]{1,1,0,3,3}), "before", Constants.print);
				PRLearner.policyReuse(false, false);
				PRLearner.policyReuse(true, false);
			}
		} else {
			//procedural testing sessions
			//continue to use qlearning with the q-values learned from training
			for(MyWorld testWorld : testingWorlds){
				QLearner testQLearner = new QLearner(Main.connect, new QValuesSet(
						trainedLearners.get(0).robotQValues, trainedLearners.get(0).jointQValues), false);
				testQLearner.run(testWorld, false, false);
				testQLearner.run(testWorld, true, false);
			}
		}
	}
	
	/**
	 * Given the training scenarios and sensor observations of the new scenario, 
	 * the robot tries to determine what's the probability of each training scenario being relevant to the new one.
	 */
	public double[] calculatePrior(List<MyWorld> trainingWorlds, MyWorld testWorld) {
		//testWorld.setWindAndDryness();
		int[][] trainingRealValues = new int[trainingWorlds.size()][MyWorld.NUM_VARIABLES];
		for(int i=0; i<trainingWorlds.size(); i++){
			trainingRealValues[i][0] = trainingWorlds.get(i).simulationWind;
			trainingRealValues[i][1] = trainingWorlds.get(i).simulationDryness;
			System.out.println(i+ " wind = "+trainingRealValues[i][0]);
			System.out.println(i+ " dryness = "+trainingRealValues[i][1]);
		}
		
		//System.out.println("obsWind "+obsWind+" obsDryness "+obsDryness);
		double[] probs = new double[trainingWorlds.size()];
		for(int i=0; i<trainingWorlds.size(); i++){
			//P(Ow|w)P(w)
			double numWind = probObsGivenWind[testWorld.simulationWind][trainingRealValues[i][0]] * probWind[trainingRealValues[i][0]];
			//System.out.println("numWind "+numWind);
			double denomWind = 0;
			for(int index=0; index<probObsGivenWind.length; index++)
				denomWind += probObsGivenWind[testWorld.simulationWind][index] * probWind[index];
			//System.out.println("denomWind "+denomWind);
			
			double numDryness = probObsGivenDryness[testWorld.simulationDryness][trainingRealValues[i][1]] * probDryness[trainingRealValues[i][1]];
			//System.out.println("numDryness "+numDryness);
			double denomDryness = 0;
			for(int index=0; index<probObsGivenDryness.length; index++)
				denomDryness += probObsGivenDryness[testWorld.simulationDryness][index] * probDryness[index];
			//System.out.println("denomDryness "+denomDryness);
			
			double probOfWind = denomWind>0?(numWind/denomWind):numWind;
			double probOfDryness = denomDryness>0?(numDryness/denomDryness):numDryness;
			probs[i] = probOfWind * probOfDryness;
		}
		double sum=0;
		for(int i=0; i<probs.length; i++)
			sum+=probs[i];
		for(int i=0; i<probs.length; i++){
			if(sum>0)
				probs[i]/=sum;
			probs[i]*=100;
		}
		
		return probs;
	}
	
	/**
	 * Initialize the prior probabilities of wind and dryness occurring 
	 * and the conditional probabilities of the observation being correct given the real values.
	 */
	public void initPriorProbabilities(){
		probWind = new double[10];
		probDryness = new double[10];
		for(int i=0; i<probWind.length; i++){
			if(i==0 || i==1 || i==8 || i==9){
				probWind[i] = 0.05;
				probDryness[i] = 0.05;
			} else if(i==2 || i==7){
				probWind[i] = 0.1;
				probDryness[i] = 0.1;
			} else{
				probWind[i] = 0.15;
				probDryness[i] = 0.15;
			}
		}
		
		probObsGivenWind = new double[10][10];
		probObsGivenDryness = new double[10][10];
		
		for(int i=0; i<probObsGivenWind.length; i++){
			for(int j=0; j<probObsGivenWind[i].length; j++){
				if(i==j)
					probObsGivenWind[i][j] = 0.4;
				else if(Math.abs(i-j) == 1)
					probObsGivenWind[i][j] = 0.2;
				else if(Math.abs(i-j) == 2)
					probObsGivenWind[i][j] = 0.1;
				//else
					//probObsGivenWind[i][j] = 0.01;
			}
		}
		for(int j=0; j<probObsGivenWind[0].length; j++){
			double sum = 0;
			for(int i=0; i<probObsGivenWind.length; i++){
				sum+=probObsGivenWind[i][j];
			}
			sum *= 100;
			sum = Math.round(sum);
			sum /= 100;
			for(int i=0; i<probObsGivenWind.length; i++){
				probObsGivenWind[i][j] /= sum;
			}
		}
		
		for(int i=0; i<probObsGivenWind.length; i++){
			for(int j=0; j<probObsGivenWind[i].length; j++){
				probObsGivenDryness[i][j] = probObsGivenWind[i][j];
				System.out.print(probObsGivenDryness[i][j]+" ");
			}
			System.out.println();
		}
	}
	
	public void calculateTestSimulationWindDryness(MyWorld world){
		int randNumWind = Tools.rand.nextInt(100);
		int randNumDryness = Tools.rand.nextInt(100);
		double sum = 0;
		int count = 0;
		System.out.println("testWorld wind "+world.testWind+" dryness "+world.testDryness);
		while(count < probObsGivenWind.length){
			sum += probObsGivenWind[count][world.testWind]*100;
			//System.out.println("sum wind "+sum);
			if(randNumWind <= sum)
				break;
			count++;
		}
		world.simulationWind = count;
		System.out.println("obsWind "+world.simulationWind);
		
		sum = 0;
		count = 0;
		while(count < probObsGivenDryness.length){
			sum += probObsGivenDryness[count][world.testDryness]*100;
			//System.out.println("sum dryness "+sum);
			if(randNumDryness <= sum)
				break;
			count++;
		}
		world.simulationDryness = count;
		System.out.println("obsDryness "+world.simulationDryness);
	}
}
