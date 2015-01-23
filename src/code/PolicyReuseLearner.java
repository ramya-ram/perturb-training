package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

/**
 * Implementation of the policy reuse algorithm
 * Given a library of Q-value functions learned from the training sessions, the robot learns how to perform a new task it hasn't seen
 * Used in the testing session of the perturbation training condition using HR-Perturb
 */
public class PolicyReuseLearner extends LearningAlgorithm {
	public PolicyReuseLearner(MyWorld myWorld, List<QValuesSet> learners){
		this.myWorld = myWorld;
		
		qValuesList = new ArrayList<QValuesSet>();
		for(QValuesSet set : learners){
			QValuesSet newSet = set.clone();
			qValuesList.add(newSet);
		}	
		timer = new Timer(1000, timerListener());
	}
	
	public Policy policyReuse(boolean withHuman, boolean computePolicy) {
		return policyReuse(withHuman, computePolicy, null);
	}
	
	/**
	 * Runs the policy reuse algorithm for the number of episodes specified
	 */
	public Policy policyReuse(boolean withHuman, boolean computePolicy, State initialStateHuman) {
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman;
		
		long start = System.currentTimeMillis();		
		int numEpisodes = Constants.NUM_EPISODES;
		if(myWorld.typeOfWorld == Constants.TESTING){
			currCommunicator = Constants.ROBOT; //robot initiates
			numEpisodes = Constants.NUM_EPISODES_TEST;
		}	
		if(withHuman)
			numEpisodes = 1;
		
		resetCommunicationCounts();		
		System.out.println("testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness);
		
		if(withHuman && Main.gameView != null){
			System.out.println("with human");
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		Policy policy = null;

		//starting policy reuse algorithm
		try{
			BufferedWriter mainWriter = new BufferedWriter(new FileWriter(new File(Constants.qvaluesDir+"mainWriter_HRPR_test_"+(myWorld.sessionNum-1)+".txt"), true));
			mainWriter.write("wind "+myWorld.testWind+" dryness "+myWorld.testDryness+"\n");
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardHRPRName), true));
			double currTemp = Constants.TEMP;
			for(int k=0; k<numEpisodes; k++){
				//choosing an action policy, giving each a probability based on the temperature parameter and the gain W
				double[] probForPolicies = getProbForPolicies(qValuesList, currTemp);
				probForPolicies = getAccumulatedArray(probForPolicies);
				
				int policyNum = 0;
				if(withHuman){
					//if working with the human, choose the policy with the highest weight
					double maxWeight = Integer.MIN_VALUE;
					policyNum = -1;
					for(int i=0; i<qValuesList.size(); i++){
						if(qValuesList.get(i).weight > maxWeight){
							maxWeight = qValuesList.get(i).weight;
							policyNum = i;
						}
						mainWriter.write(i+" "+qValuesList.get(i).weight+"\n");
					}
					mainWriter.write("finally using "+policyNum+"\n");
					System.out.println("working with human, best policy = "+policyNum);
				} else {
					int randNum = Tools.rand.nextInt(100);
					while(randNum>probForPolicies[policyNum]){
						policyNum++;
						if(policyNum>=probForPolicies.length){
							policyNum = probForPolicies.length-1;
							break;
						}
					}
				}
				double reward = 0;
				int iterations = 0;
				long duration = 0;
				
				currQValues = qValuesList.get(policyNum);
				Tuple<Double, Integer, Long> tuple = run(Constants.NUM_STEPS_PER_EPISODE, initialStateHuman);
				reward = tuple.getFirst();
				iterations = tuple.getSecond();
				duration = tuple.getThird();

				if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(reward, iterations, duration);
					else{
						if(myWorld.typeOfWorld == Constants.TESTING)
							rewardWriter.write(""+reward+", ");
					}
				}
	           
				currQValues = qValuesList.get(policyNum);
				currQValues.weight = (currQValues.weight*currQValues.numEpisodesChosen + reward)/(currQValues.numEpisodesChosen + 1);
				currQValues.numEpisodesChosen = currQValues.numEpisodesChosen + 1;
				currTemp = currTemp + Constants.DELTA_TEMP;
			}
			rewardWriter.close();
			
			if(computePolicy)
				policy = computePolicy();
			long end = System.currentTimeMillis();
			if(myWorld.typeOfWorld == Constants.TESTING && !withHuman){
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(Constants.simulationDir+"duration"+Constants.NUM_EPISODES_TEST+".csv"), true));
				System.out.println("policyReuse duration "+(end-start));
				writer.write((end-start)+"\n");
				writer.close();
			}
			mainWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}	
		return policy;
	}
	
	public void printWeights(){
		for(QValuesSet set : qValuesList)
			System.out.print(set.weight+" ");
		System.out.println();
	}
	
	public void printNumEpisodesChosen(){
		for(QValuesSet set : qValuesList)
			System.out.print(set.numEpisodesChosen+" ");
		System.out.println();
	}
	
	/**
	 * An array of probabilities is calculated for the policies so that one can be chosen based on the weights associated with each
	 */
	public double[] getProbForPolicies(List<QValuesSet> learners, double temp) {
		double[] probForPolicies = new double[learners.size()];
		double sum = 0;
		for(int i=0; i<probForPolicies.length; i++){
			probForPolicies[i] = Math.pow(Math.E, temp*learners.get(i).weight);
			sum += probForPolicies[i];
		}
		for(int i=0; i<probForPolicies.length; i++){
			if(sum > 0){
				probForPolicies[i] /= sum;
			}
			probForPolicies[i] *= 100;
		}
		return probForPolicies;
	}
	
	public double[] getAccumulatedArray(double[] probForPolicies){
		for(int i=1; i<probForPolicies.length; i++){
			probForPolicies[i] = probForPolicies[i-1]+probForPolicies[i];
		}
		return probForPolicies;
	}
	
	public boolean isPastPolicy(int index) {
		return index > 0; //the first (index 0) policy is the current policy
	}
}
