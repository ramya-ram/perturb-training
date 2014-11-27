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
	
	public PolicyReuseLearner(MyWorld myWorld, SocketConnect connect, double gamma, double alpha, PolicyLibrary library, QValuesSet qValuesSet, double[] policyWeights){
		this.connect = connect;
		this.myWorld = myWorld;
		this.gamma = gamma;
		this.alpha = alpha;
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
		weights[library.size()] = 0;//100;
		System.out.println("weights["+library.size()+"] = "+weights[library.size()]);
	}
	
	/**
	 * Runs the policy reuse algorithm for the number of episodes specified
	 */
	public Policy policyReuse(double temp, double deltaTemp, 
			int maxEpisodes, int maxSteps, double probPast, double decayValue, double gamma, double alpha, String label, 
			boolean withHuman, boolean computePolicy) {
		this.mdp = MyWorld.mdp;
		Main.currWithSimulatedHuman = withHuman;
		if(withHuman){
			this.epsilon = Main.HUMAN_EPSILON;
			//Main.humanInteractionNum++;
		} else {
			this.epsilon = Main.SIMULATION_EPSILON;
		}
		if(myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM)
			currCommunicator = 1; //robot initiates
		
		numRobotSuggestions = 0;
		numRobotUpdates = 0;
		numHumanSuggestions = 0;
		numHumanUpdates = 0;
		
		numRobotAccepts = 0;
		numRobotRejects = 0;
		numHumanAccepts = 0;
		numHumanRejects = 0;
		
		System.out.println("myWorld "+myWorld.trainingSessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		//starting policy reuse algorithm
		System.out.println("weights: ");
		printArray(weights);
		System.out.println("num of episodes chosen: ");
		printArray(numOfEpisodesChosen);
		try{
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Main.rewardPerturbName), true));
			double currTemp = temp;
			double cumulativeReward = 0;
			double cumulativeIter = 0;
			for(int k=0; k<maxEpisodes; k++){
				//choosing an action policy, giving each a probability based on the temperature parameter and the gain W
				double[] probForPolicies = getProbForPolicies(weights, currTemp);
				int policyNum = 0;
				if(Main.currWithSimulatedHuman){
					System.out.println("using current policy");
					policyNum = probForPolicies.length-1;
				} else {
					int randNum = Main.rand.nextInt(100);
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
					Tuple<Double, Integer, Long> tuple = piReuse(currPolicy, 1, maxSteps, probPast, decayValue);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				} else {
					System.out.println("using curr policy");
					Tuple<Double, Integer, Long> tuple = runFullyGreedy(maxSteps);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				}
				cumulativeReward += reward;
				cumulativeIter += iterations;
				if(Main.currWithSimulatedHuman && Main.saveToFile && (myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM)){
					rewardWriter.write(""+reward+", ");
				}
	           
				weights[policyNum] = (weights[policyNum]*numOfEpisodesChosen[policyNum] + reward)/(numOfEpisodesChosen[policyNum] + 1);
				numOfEpisodesChosen[policyNum] = numOfEpisodesChosen[policyNum] + 1;
				currTemp = currTemp + deltaTemp;
				
				System.out.println("weights: ");
				printArray(weights);
				System.out.println("num of episodes chosen: ");
				printArray(numOfEpisodesChosen);
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
		        	//System.out.println("visited state "+state+" id "+state.getId());//Main.stateToId.get(state.initialId));//Main.stateToId.get(state));
					//if(myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM)
		        	//	System.out.println("state "+state.toStringFile());
					HumanRobotActionPair agentActions = null;
					int randNum = Main.rand.nextInt(100);
					if(randNum < currProbPast){
						//if(withHuman && !reachedGoalState){
						//	System.out.println("past policy action "+pastPolicy.action(state.getId()));
		        		//	agentActions = getAgentActionsCommWithHuman(state, pastPolicy.action(state.getId()).getRobotAction());
						//}
		        		//else
		        			agentActions = pastPolicy.action(state.getId());
					} 
					if(randNum >= currProbPast || agentActions == null){
		        		//if(withHuman && !reachedGoalState)
		        		//	agentActions = getAgentActionsCommWithHuman(state, null);
		        		//else
		        			agentActions = getAgentActionsSimulation(state);
					}  
					
					State nextState = myWorld.getNextState(state, agentActions);					                
					reward[k][count] = myWorld.reward(state, agentActions, nextState);
					episodeReward += reward[k][count];					
					saveEpisodeToFile(state, agentActions.getHumanAction(), agentActions.getRobotAction(), nextState, reward[k][count]);
					updateQValues(state, agentActions, nextState, reward[k][count]);
		            
//					if(myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM){
//		            	System.out.println(state.toStringFile()+": "+agentActions+" = "+reward[k][count]);
//		            }
					
					currProbPast = currProbPast*decayValue;
					state = nextState.clone();
					count++;
					
					if(!reachedGoalState){
						if(MyWorld.isGoalState(state)){
							iterations = count;
							reachedGoalState = true;
						}		
					}
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			
			long endTime = System.currentTimeMillis();
			duration = endTime - startTime;
		}
		/*double averageGain = 0;
		for(int k=0; k<numEpisodes; k++){
			for(int h=0; h<numSteps; h++){
				averageGain += Math.pow(gamma, h) * reward[k][h];
			}
		}
		averageGain *= 1/((double)numEpisodes);*/
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
	
	public void printArray(double[] arr){
		for(int i=0; i<arr.length; i++)
			System.out.print(arr[i]+", ");
		System.out.println();
	}
	
	public void printArray(int[] arr){
		for(int i=0; i<arr.length; i++)
			System.out.print(arr[i]+", ");
		System.out.println();
	}
}
