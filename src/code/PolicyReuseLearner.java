package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.Timer;

/**
 * Implementation of the policy reuse algorithm
 * Given a library of policies learned from the training sessions, the robot learns how to perform a new task it hasn't seen
 * Used in the testing session of the perturbation training condition
 */
public class PolicyReuseLearner extends LearningAlgorithm {
	public double[] weights;
	public int[] numOfEpisodesChosen;
	public PolicyLibrary library;
	
	public PolicyReuseLearner(MyWorld myWorld, SocketConnect connect, PolicyLibrary library, QValuesSet qValuesSet, double[] policyWeights){
		this.connect = connect;
		this.myWorld = myWorld;
		this.library = library;
		timer = new Timer(1000, timerListener());
		
		robotQValues = qValuesSet.getRobotQValues();
		jointQValues = qValuesSet.getJointQValues();
		weights = new double[library.size()+1];
		numOfEpisodesChosen = new int[library.size()+1];
		for(int i=0; i<weights.length-1; i++){
			weights[i] = policyWeights[i];
			numOfEpisodesChosen[i] = 0;
			System.out.println("weights["+i+"] = "+weights[i]);
		}
		weights[library.size()] = 0;
		System.out.println("weights["+library.size()+"] = "+weights[library.size()]);
	}
	
	/**
	 * Runs the policy reuse algorithm for the number of episodes specified
	 */
	public Policy policyReuse(boolean withHuman, boolean computePolicy) {
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman;
//		if(withHuman){
//			this.epsilon = Main.HUMAN_EPSILON;
//			//Main.humanInteractionNum++;
//		} else {
//			this.epsilon = Main.SIMULATION_EPSILON;
//		}
		if(withHuman && Main.CURRENT_EXECUTION == Main.SIMULATION)
			return null;
		myWorld.setWindAndDryness();
		if(myWorld.typeOfWorld == Constants.TESTING)
			currCommunicator = Constants.ROBOT; //robot initiates
		int numEpisodes = Constants.NUM_EPISODES;
		if(withHuman)
			numEpisodes = 1;
		
		resetCommunicationCounts();
		
		System.out.println("myWorld typeOfWorld "+myWorld.typeOfWorld+" sessionNum "+myWorld.sessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		if(withHuman && Main.connect != null){
			Main.st.server.startRound.setEnabled(true);
			while(!Main.st.server.startClicked){
				System.out.print("");
			}
			Main.st.server.startRound.setEnabled(false);
			Main.st.server.startClicked = false;
		}
		
		//starting policy reuse algorithm
		System.out.println("weights: ");
		Tools.printArray(weights);
		System.out.println("num of episodes chosen: ");
		Tools.printArray(numOfEpisodesChosen);
		try{
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardPerturbName), true));
			double currTemp = Constants.TEMP;
			//double cumulativeReward = 0;
			//double cumulativeIter = 0;
			for(int k=0; k<numEpisodes; k++){
				//choosing an action policy, giving each a probability based on the temperature parameter and the gain W
				double[] probForPolicies = getProbForPolicies(weights, currTemp);
				int policyNum = 0;
				if(withHuman){
					System.out.println("using current policy");
					policyNum = probForPolicies.length-1;
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
					System.out.println("using policy num "+policyNum);
					Policy currPolicy = library.get(policyNum);
					Tuple<Double, Integer, Long> tuple = piReuse(currPolicy, 1, Constants.NUM_STEPS_PER_EPISODE, 
							Constants.PAST_PROB, Constants.DECAY_VALUE);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				} else {
					System.out.println("using curr policy");
					Tuple<Double, Integer, Long> tuple = runFullyGreedy(Constants.NUM_STEPS_PER_EPISODE);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				}
				//cumulativeReward += reward;
				//cumulativeIter += iterations;
				/*if(Main.saveToFile){
					rewardWriter.write(""+(cumulativeReward/(k+1))+", ");
		            iterWriter.write(""+(cumulativeIter/(k+1))+", ");
				}*/
				if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(reward, iterations, duration);
					else{
						if(myWorld.typeOfWorld == Constants.TESTING)
							rewardWriter.write(""+reward+", ");
					}
				}
	           
				weights[policyNum] = (weights[policyNum]*numOfEpisodesChosen[policyNum] + reward)/(numOfEpisodesChosen[policyNum] + 1);
				numOfEpisodesChosen[policyNum] = numOfEpisodesChosen[policyNum] + 1;
				currTemp = currTemp + Constants.DELTA_TEMP;
				
				System.out.println("weights: ");
				Tools.printArray(weights);
				System.out.println("num of episodes chosen: ");
				Tools.printArray(numOfEpisodesChosen);
			}
			rewardWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		if(computePolicy)
			return computePolicy();
		return null;
	}
	
	/**
	 * Given a past policy, runs an episode that either chooses the past policy action or uses a e-greedy approach
	 */
	public Tuple<Double, Integer, Long> piReuse(
			Policy pastPolicy, int numEpisodes, int numSteps, double probPast, double decayValue) {
		//double[][] robotQValuesPiReuse = new double[MyWorld.mdp.states.size()][Action.values().length];
		//double[][][] jointQValuesPiReuse = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
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
						if(withHuman && !reachedGoalState && Main.CURRENT_EXECUTION != Main.SIMULATION){
						//	System.out.println("past policy action "+pastPolicy.action(state.getId()));
		        			agentActions = getAgentActionsCommWithHuman(state, pastPolicy.action(state.getId()).getRobotAction());
						}
		        		else
		        			agentActions = pastPolicy.action(state.getId());
					} 
					if(randNum >= currProbPast || agentActions == null){
		        		if(withHuman && !reachedGoalState && Main.CURRENT_EXECUTION != Main.SIMULATION)
		        			agentActions = getAgentActionsCommWithHuman(state, null);
		        		else
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
							if(withHuman && connect != null){
								connect.sendMessage("-------------------------------------\nCONGRATS! You and your teammate have completed this round!\n"
										+ "-------------------------------------\nPLEASE CLICK NEXT AND THEN ANSWER QUESTIONS!"); 
							}
						}
						if(withHuman){
							if(connect != null){
								enableNextButton();
								waitForClick();
							}
							if(Main.gameView != null){
								Main.gameView.setNextEnable(true);
								Main.gameView.waitForNextClick();
								if(reachedGoalState){
									Main.gameView.initTitleGUI("congrats");
								}
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
	public Tuple<Double, Integer, Long> runFullyGreedy(int maxSteps) {
		Tuple<Double, Integer, Long> tuple = run(true /*fullyGreedy*/, maxSteps);
        return new Tuple<Double, Integer, Long>(tuple.getFirst(), tuple.getSecond(), tuple.getThird());
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
	
	public boolean isPastPolicy(PolicyLibrary library, int index) {
		return index<library.size();
	}
}
