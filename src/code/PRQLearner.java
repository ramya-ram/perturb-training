package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import javax.swing.Timer;

public class PRQLearner extends LearningAlgorithm {
	public double[] weights; //stores the weights for each prior policy and for the value function being currently learned
	public int[] numOfEpisodesChosen; //stores for how many episodes each prior policy and the current value function has been used
	public List<Policy> library; //stores the library of previously learned policies
	
	public PRQLearner(MyWorld myWorld, List<Policy> library, QValuesSet qValuesSet){
		this.myWorld = myWorld;
		this.library = library;
		timer = new Timer(1000, timerListener());
		if(qValuesSet != null) //transfer the previously learned Q-values if not null
			currQValues = qValuesSet.clone();
		else //if there are no Q-values to transfer from previous tasks, use the ones from offline learning
			currQValues = new QValuesSet(Main.robotQValuesOffline, Main.jointQValuesOffline);
		
		weights = new double[library.size()+1];
		numOfEpisodesChosen = new int[library.size()+1];
		for(int i=0; i<weights.length-1; i++){
			weights[i] = 0;
			numOfEpisodesChosen[i] = 0;
		}
		weights[library.size()] = 0;
	}
	
	/**
	 * Runs the Policy Reuse in Q-learning (PRQL) algorithm for the MDP specified by MyWorld in the constructor
	 * initialStateHuman is null so the initial state will be randomly selected
	 */
	public void runPRQL(boolean withHuman) {
		runPRQL(withHuman, null);
	}
	
	/**
	 * Runs the Policy Reuse in Q-learning (PRQL) algorithm for the MDP specified by MyWorld in the constructor and use initialStateHuman as the initial state
	 */
	public Policy runPRQL(boolean withHuman, State initialStateHuman) {
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman;
		
		int numEpisodes = Constants.NUM_EPISODES; //run Constants.NUM_EPISODES episodes when running any training task execution
		if(myWorld.typeOfWorld == Constants.TESTING){
			currCommunicator = Constants.ROBOT; //robot initiates
			numEpisodes = Constants.NUM_EPISODES_TEST; //run Constants.NUM_EPISODES_TEST episodes when running any test task execution
		}	
		if(withHuman) //only run one episode when working with the person
			numEpisodes = 1;
		
		resetCommunicationCounts();
				
		if(withHuman && Main.gameView != null){
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		
		try{
			String fileName = "";
			if(Main.SUB_EXECUTION == Main.REWARD_OVER_ITERS)
				fileName = Constants.numIterName;
			else
				fileName = Constants.rewardPRQLName;
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(fileName), true));
			double currTemp = Constants.TEMP;
			for(int k=0; k<numEpisodes; k++){
				//choosing a policy for action selection, giving each a probability based on the temperature parameter and weights
				double[] probForPolicies = getProbForPolicies(weights, currTemp);
				int policyNum = 0;
				//use the value function that is currently being learned when evaluating PRQL
				if(withHuman || (Main.SUB_EXECUTION == Main.REWARD_OVER_ITERS && k%Constants.INTERVAL == 0)){
					policyNum = probForPolicies.length-1; //the new value function being learned
				} else { //otherwise sample a policy (or the new value function) for use in this episode
					int randNum = Constants.rand.nextInt(100);
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
				if(isPastPolicy(library, policyNum)){ //using a past policy
					Policy currPolicy = library.get(policyNum);
					Tuple<Double, Integer, Long> tuple = piReuse(currPolicy, 1, Constants.NUM_STEPS_PER_EPISODE, 
							Constants.PAST_PROB, Constants.DECAY_VALUE);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				} else { //using the new value function being learned, running a full greedy episode (no exploration)
					Tuple<Double, Integer, Long> tuple = runFullyGreedy(Constants.NUM_STEPS_PER_EPISODE, initialStateHuman);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				}
				
				//if trying to get a learning curve of the agent, store the reward if one interval has passed
				//so if the interval = 100, store the reward every 100 episodes
				//add this reward to the reward from previous simulation runs (at the end, we will divide by the number of runs to get an average learning curve)
				if(Main.SUB_EXECUTION == Main.REWARD_OVER_ITERS){
					if(myWorld.typeOfWorld == Constants.TESTING && k%Constants.INTERVAL == 0)
						Main.PRQLTotal[myWorld.sessionNum-1][(k/Constants.INTERVAL)] += reward;
				} else {
					if(withHuman && Main.saveToFile){
						if(Main.CURRENT_EXECUTION != Main.SIMULATION)
							saveDataToFile(reward, iterations, duration);
						else{
							if(myWorld.typeOfWorld == Constants.TESTING)
								rewardWriter.write(""+reward+", ");
						}
					}
				}
	           
				//the weight of the policy/value function chosen for this episode is updated
				//and the number of times it been chosen is incremented
				weights[policyNum] = (weights[policyNum]*numOfEpisodesChosen[policyNum] + reward)/(numOfEpisodesChosen[policyNum] + 1);
				numOfEpisodesChosen[policyNum] = numOfEpisodesChosen[policyNum] + 1;
				currTemp = currTemp + Constants.DELTA_TEMP;
			}
			rewardWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Given a past policy, runs an episode that either chooses the past policy action or uses a e-greedy approach
	 */
	public Tuple<Double, Integer, Long> piReuse(
			Policy pastPolicy, int numEpisodes, int numSteps, double probPast, double decayValue) {
		double[][] reward = new double[numEpisodes][numSteps+1];
		double episodeReward = 0;
		int iterations = 0;
		long duration = 0;
		for(int k=0; k<numEpisodes; k++){
			State state = myWorld.initialState().clone();
			double currProbPast = probPast*100;
			long startTime = System.currentTimeMillis();
			try{
				while(!myWorld.isGoalState(state) && iterations < numSteps){
					HumanRobotActionPair agentActions = null;
					int randNum = Constants.rand.nextInt(100);
					if(randNum < currProbPast){
						if(withHuman && Main.CURRENT_EXECUTION != Main.SIMULATION)
		        			agentActions = getAgentActionsCommWithHuman(state); //communicates with human to choose action
		        		else
		        			agentActions = pastPolicy.action(state.getId()); //agent chooses the action specified by the past policy
					} 
					if(randNum >= currProbPast || agentActions == null){
		        		if(withHuman && Main.CURRENT_EXECUTION != Main.SIMULATION)
		        			agentActions = getAgentActionsCommWithHuman(state); //communicates with human to choose action
		        		else
		        			agentActions = getAgentActionsSimulation(state); //agent uses e-greedy approach to choose action
					}  
					
					State nextState = myWorld.getNextState(state, agentActions);					                
					reward[k][iterations] = myWorld.reward(state, agentActions, nextState);
					episodeReward += reward[k][iterations];					
					saveEpisodeToFile(state, agentActions.getHumanAction(), agentActions.getRobotAction(), nextState, reward[k][iterations]);
					updateQValues(state, agentActions, nextState, reward[k][iterations]);
					
					currProbPast = currProbPast*decayValue; //decays the probability of using a past policy (as the agent learns, it's more likely to choose the new value function being learned)
					state = nextState.clone();
					iterations++;
					
					if(withHuman && Main.gameView != null){
						Main.gameView.setNextEnable(true);
						Main.gameView.waitForNextClick();
						if(myWorld.isGoalState(state)){
							Main.gameView.initTitleGUI("congrats");
						}
					}
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			
			long endTime = System.currentTimeMillis();
			duration = endTime - startTime;
		}
		return new Tuple<Double, Integer, Long>(episodeReward, iterations, duration);
	}
	
	/**
	 * Run Q-learning, use initialStateHuman as the initial state
	 */
	public Tuple<Double, Integer, Long> runFullyGreedy(int maxSteps, State initialStateHuman) {
		return run(true, maxSteps, initialStateHuman);
    }
	
	/**
	 * A distribution is calculated over all the policies and new value function so that for each episode, one can be sampled for action selection
	 * The higher the weight, the more likely it will be chosen for that episode
	 */
	public double[] getProbForPolicies(double[] weights, double temp) {
		double[] probForPolicies = new double[weights.length];
		double sum = 0;
		for(int i=0; i<probForPolicies.length; i++){
			probForPolicies[i] = Math.pow(Math.E, temp*weights[i]);
			sum += probForPolicies[i];
		}
		for(int i=0; i<probForPolicies.length; i++){
			if(sum > 0)
				probForPolicies[i] /= sum;
			probForPolicies[i] *= 100;
		}
		for(int i=1; i<probForPolicies.length; i++){
			probForPolicies[i] = probForPolicies[i-1]+probForPolicies[i];
		}
		return probForPolicies;
	}
	
	/**
	 * All policies from 0 to library.size()-1 are past policies, the one at library.size() (or weights.size()-1) is the new value function being learned
	 * (weights.size() = library.size() + 1)
	 */
	public boolean isPastPolicy(List<Policy> library, int index) {
		return index<library.size();
	}
}
