package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import javax.swing.Timer;

public class PRQLearner extends LearningAlgorithm {
	public double[] weights;
	public int[] numOfEpisodesChosen;
	public List<Policy> library;
	
	public PRQLearner(MyWorld myWorld, List<Policy> library, QValuesSet qValuesSet){
		this.myWorld = myWorld;
		this.library = library;
		timer = new Timer(1000, timerListener());
		
		if(qValuesSet != null) //transfer the previously learned q-values passed in as a parameter if not null
			currQValues = qValuesSet.clone();
		else //if there are no qvalues to transfer from previous tasks, use the ones from offline learning
			currQValues = new QValuesSet(Main.robotQValuesOffline, Main.jointQValuesOffline);
		
		weights = new double[library.size()+1];
		numOfEpisodesChosen = new int[library.size()+1];
		for(int i=0; i<weights.length-1; i++){
			weights[i] = 0;
			numOfEpisodesChosen[i] = 0;
		}
		weights[library.size()] = 0;
	}
	
	public void runPRQL(boolean withHuman) {
		runPRQL(withHuman, null);
	}
	
	/**
	 * Runs the policy reuse algorithm for the number of episodes specified
	 */
	public Policy runPRQL(boolean withHuman, State initialStateHuman) {
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman;
		
		int numEpisodes = Constants.NUM_EPISODES;
		if(myWorld.typeOfWorld == Constants.TESTING){
			currCommunicator = Constants.ROBOT; //robot initiates
			numEpisodes = Constants.NUM_EPISODES_TEST;
		}	
		if(withHuman)
			numEpisodes = 1;
		
		resetCommunicationCounts();
		
		//System.out.println("myWorld typeOfWorld "+myWorld.typeOfWorld+" sessionNum "+myWorld.sessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		if(withHuman && Main.gameView != null){
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		
		//starting policy reuse algorithm
//		System.out.println("weights: ");
//		Tools.printArray(weights);
//		System.out.println("num of episodes chosen: ");
//		Tools.printArray(numOfEpisodesChosen);
		try{
			//BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPRQLName), true));
			double currTemp = Constants.TEMP;
			for(int k=0; k<numEpisodes; k++){
				//choosing an action policy, giving each a probability based on the temperature parameter and the gain W
				double[] probForPolicies = getProbForPolicies(weights, currTemp);
				int policyNum = 0;
				if(withHuman){
					policyNum = probForPolicies.length-1; //the new policy
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
				if(isPastPolicy(library, policyNum)){
					//System.out.println("using policy num "+policyNum);
					Policy currPolicy = library.get(policyNum);
					Tuple<Double, Integer, Long> tuple = piReuse(currPolicy, 1, Constants.NUM_STEPS_PER_EPISODE, 
							Constants.PAST_PROB, Constants.DECAY_VALUE);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				} else {
					//System.out.println("using curr policy");
					Tuple<Double, Integer, Long> tuple = runFullyGreedy(Constants.NUM_STEPS_PER_EPISODE, initialStateHuman);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				}
				
				/*if(myWorld.typeOfWorld == Constants.TESTING && k%100 == 0){
					BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.numIterName+"_"+myWorld.sessionNum+".csv"), true));
					rewardWriter.write(""+reward+", ");
					rewardWriter.close();
				}*/
				
				if(myWorld.typeOfWorld == Constants.TESTING && k%Constants.INTERVAL == 0){
					Main.PRQLTotal[myWorld.sessionNum-1][(k/Constants.INTERVAL)] += reward;
					//System.out.print(reward+", ");
				}
				
				if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(reward, iterations, duration);
					else{
						
					}
				}
	           
				weights[policyNum] = (weights[policyNum]*numOfEpisodesChosen[policyNum] + reward)/(numOfEpisodesChosen[policyNum] + 1);
				numOfEpisodesChosen[policyNum] = numOfEpisodesChosen[policyNum] + 1;
				currTemp = currTemp + Constants.DELTA_TEMP;
				
//				System.out.println("weights: ");
//				Tools.printArray(weights);
//				System.out.println("num of episodes chosen: ");
//				Tools.printArray(numOfEpisodesChosen);
			}
			//System.out.println();
			//rewardWriter.close();
			/*BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.numIterName+"_"+myWorld.sessionNum+".csv"), true));
			rewardWriter.write("\n");
			rewardWriter.close();*/
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
		int count = 0;
		long duration = 0;
		for(int k=0; k<numEpisodes; k++){
			boolean reachedGoalState = false;
			State state = myWorld.initialState().clone();
			double currProbPast = probPast*100;
			long startTime = System.currentTimeMillis();
			try{
				while(!MyWorld.isGoalState(state) && count < numSteps){
					HumanRobotActionPair agentActions = null;
					int randNum = Tools.rand.nextInt(100);
					if(randNum < currProbPast){
						//if(withHuman && !reachedGoalState && Main.CURRENT_EXECUTION != Main.SIMULATION){
		        		//	agentActions = getAgentActionsCommWithHuman(state, pastPolicy.action(state.getId()).getRobotAction());
						//}
		        		//else
		        			agentActions = pastPolicy.action(state.getId());
					} 
					if(randNum >= currProbPast || agentActions == null){
		        		//if(withHuman && !reachedGoalState && Main.CURRENT_EXECUTION != Main.SIMULATION)
		        		//	agentActions = getAgentActionsCommWithHuman(state, null);
		        		//else
		        			agentActions = getAgentActionsSimulation(state);
					}  
					
					State nextState = myWorld.getNextState(state, agentActions);					                
					reward[k][count] = myWorld.reward(state, agentActions, nextState);
					episodeReward += reward[k][count];					
					saveEpisodeToFile(state, agentActions.getHumanAction(), agentActions.getRobotAction(), nextState, reward[k][count]);
					updateQValues(state, agentActions, nextState, reward[k][count]);
					
					currProbPast = currProbPast*decayValue;
					state = nextState.clone();
					count++;
					
					if(!reachedGoalState){
						if(MyWorld.isGoalState(state)){
							iterations = count;
							reachedGoalState = true;
						}
						if(withHuman && Main.gameView != null){
							Main.gameView.setNextEnable(true);
							Main.gameView.waitForNextClick();
							if(reachedGoalState){
								Main.gameView.initTitleGUI("congrats");
							}
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
	 * Run QLearning for the number of episodes specified and see how accumulated reward changes over these episodes
	 */
	public Tuple<Double, Integer, Long> runFullyGreedy(int maxSteps, State initialStateHuman) {
		return run(true, maxSteps, initialStateHuman);
    }
	
	/**
	 * An array of probabilities is calculated for the policies so that one can be chosen based on the weights associated with each
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
	
	public boolean isPastPolicy(List<Policy> library, int index) {
		return index<library.size();
	}
}
