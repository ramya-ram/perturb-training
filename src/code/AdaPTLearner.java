package code;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

/**
 * Implementation of the HR-Perturb algorithm that extends the policy reuse (PRQL) algorithm
 * Given a library of Q-value functions learned from the training sessions, the robot learns how to perform a new task it hasn't seen
 * Used in the testing session of the perturbation training condition using HR-Perturb
 */
public class AdaPTLearner extends LearningAlgorithm {
	public AdaPTLearner(MyWorld myWorld, List<QValuesSet> learners, ExperimentCondition condition){
		this.myWorld = myWorld;
		this.condition = condition;
		
		//list of Q-value functions that will be adapted for the new task
		qValuesList = new ArrayList<QValuesSet>();
		for(QValuesSet set : learners){
			QValuesSet newSet = set.clone();
			qValuesList.add(newSet); //adding Q-value functions learned from previous tasks
		}	
		timer = new Timer(1000, timerListener());
	}
	
	/**
	 * Runs the AdaPT algorithm with no specified initial state
	 */
	public void runAdaPT(boolean withHuman) {
		runAdaPT(withHuman, null);
	}
	
	/**
	 * Runs the AdaPT algorithm with a specific initial state (useful for consistency in human subject experiments)
	 */
	public void runAdaPT(boolean withHuman, State initialStateHuman) {
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman; 
		
		int numEpisodes = Constants.NUM_EPISODES;
		if(myWorld.typeOfWorld == Constants.TESTING){
			currCommunicator = Constants.ROBOT; //robot initiates for testing tasks to keep constant across participants
			numEpisodes = Constants.NUM_EPISODES_TEST;
		}	
		if(withHuman)
			numEpisodes = 1; //only run the task once when working with the person
		
		resetCommunicationCounts();		
		
		if(withHuman && Main.gameView != null){
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}

		//starting AdaPT algorithm
		double currTemp = Constants.TEMP;
		long start = System.currentTimeMillis();
		for(int k=0; k<numEpisodes; k++){
			//calculate probabilities of selecting each value function based on the temperature parameter and the weights
			double[] probForValueFuncs = getProbForValueFuncs(qValuesList, currTemp);
			probForValueFuncs = getAccumulatedArray(probForValueFuncs);
			
			int currValueFuncNum = 0;
			//if working with the human, choose the value function with the highest weight
			//or when calculating reward over time in simulation and one interval has passed, record the reward by using the value function with the highest weight
			if(withHuman || (Main.SUB_EXECUTION == Main.REWARD_OVER_ITERS && k%Constants.INTERVAL == 0)){ 
				double maxWeight = Integer.MIN_VALUE;
				currValueFuncNum = -1;
				for(int i=0; i<qValuesList.size(); i++){
					if(qValuesList.get(i).weight > maxWeight){
						maxWeight = qValuesList.get(i).weight;
						currValueFuncNum = i;
					}
				}
				if(withHuman){
					Main.closestTrainingTask[condition.ordinal()][myWorld.sessionNum-1] = currValueFuncNum;
					System.out.println("task "+(myWorld.sessionNum-1)+" closestMDP "+currValueFuncNum);
				}
			} else { //otherwise, choose a value function for action selection by sampling based on the probabilities
				int randNum = Constants.rand.nextInt(100);
				while(randNum > probForValueFuncs[currValueFuncNum]){
					currValueFuncNum++;
					if(currValueFuncNum >= probForValueFuncs.length){
						currValueFuncNum = probForValueFuncs.length-1;
						break;
					}
				}
			}
			double reward = 0;
			int iterations = 0;
			long duration = 0;
			
			//use the chosen value function to run an episode
			currQValues = qValuesList.get(currValueFuncNum);
			Tuple<Double, Integer, Long> tuple = run(Constants.NUM_STEPS_PER_EPISODE, initialStateHuman, k, null);
			reward = tuple.getFirst();
			iterations = tuple.getSecond();
			duration = tuple.getThird();

			if(Main.SUB_EXECUTION == Main.REWARD_OVER_ITERS){
				if(myWorld.typeOfWorld == Constants.TESTING && k%Constants.INTERVAL == 0){
					writeToFile(Constants.rewardOverItersData+"_"+condition+".csv", reward+",");
					Main.rewardOverTime[condition.ordinal()][myWorld.sessionNum-1][(k/Constants.INTERVAL)] += reward;
				}
			} else {
				if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(reward, iterations, duration);
					else{
						if(myWorld.typeOfWorld == Constants.TESTING){
							writeToFile(Constants.rewardLimitedTimeData, ""+reward+",");
							Main.rewardLimitedTime[condition.ordinal()][myWorld.sessionNum-1] += reward;
						}
					}
				}
			}
			
			//update the weight of the chosen value function and the number of times it has been used
			currQValues = qValuesList.get(currValueFuncNum);
			currQValues.weight = (currQValues.weight*currQValues.numEpisodesChosen + reward)/(currQValues.numEpisodesChosen + 1);
			currQValues.numEpisodesChosen = currQValues.numEpisodesChosen + 1;
			currTemp = currTemp + Constants.DELTA_TEMP;
		}
		long end = System.currentTimeMillis();
		long duration = end-start;
		writeToFile(Constants.duration+"_"+condition+".csv", duration+"\n");
	}
	
	/**
	 * An array of probabilities is calculated using the temperature parameter and the value function weights
	 * Using this distribution, a value function can be sampled
	 */
	public double[] getProbForValueFuncs(List<QValuesSet> learners, double temp) {
		double[] probForValueFuncs = new double[learners.size()];
		double sum = 0;
		for(int i=0; i<probForValueFuncs.length; i++){
			probForValueFuncs[i] = Math.pow(Math.E, temp*learners.get(i).weight);
			sum += probForValueFuncs[i];
		}
		for(int i=0; i<probForValueFuncs.length; i++){
			if(sum > 0){
				probForValueFuncs[i] /= sum;
			}
			probForValueFuncs[i] *= 100;
		}
		return probForValueFuncs;
	}
	
	/**
	 * Accumulate the probabilities so sampling is easier
	 * e.g. A probability distribution like [10,50,40] would become [10,60,100] 
	 */
	public double[] getAccumulatedArray(double[] probForValueFuncs){
		for(int i=1; i<probForValueFuncs.length; i++){
			probForValueFuncs[i] = probForValueFuncs[i-1]+probForValueFuncs[i];
		}
		return probForValueFuncs;
	}
}
