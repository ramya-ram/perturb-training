package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

/**
 * Implementation of the policy reuse algorithm
 * Given a library of policies learned from the training sessions, the robot learns how to perform a new task it hasn't seen
 * Used in the testing session of the perturbation training condition
 */
public class PolicyReuseLearner extends LearningAlgorithm {
	public PolicyLibrary library;
	
	public PolicyReuseLearner(MyWorld myWorld, PolicyLibrary library, QValuesSet qValuesSet, double[] policyWeights){
		this.myWorld = myWorld;
		this.library = new PolicyLibrary();
		this.library.add(new Policy()); //current policy being learned
		this.library.addAll(library.policyLibrary);
				
		for(Policy policy : this.library.policyLibrary){
			policy.weight = 0;
			policy.numEpisodesChosen = 0;
		}
		
		timer = new Timer(1000, timerListener());		
		robotQValues = qValuesSet.getRobotQValues();
		jointQValues = qValuesSet.getJointQValues();
	}
	
	/**
	 * Runs the policy reuse algorithm for the number of episodes specified
	 */
	public Policy policyReuse(boolean withHuman, boolean computePolicy) {
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman;
		
		long start = System.currentTimeMillis();
		myWorld.setWindAndDryness();
		
		int numEpisodes = Constants.NUM_EPISODES;
		if(myWorld.typeOfWorld == Constants.TESTING){
			currCommunicator = Constants.ROBOT; //robot initiates
			numEpisodes = Constants.NUM_EPISODES_TEST;
		}	
		if(withHuman)
			numEpisodes = 1;
		
		resetCommunicationCounts();
		
		//System.out.println("myWorld typeOfWorld "+myWorld.typeOfWorld+" sessionNum "+myWorld.sessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		System.out.println("wind="+myWorld.testWind+" dryness="+myWorld.testDryness);
		
		if(withHuman && Main.gameView != null){
			System.out.println("with human");
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		Policy policy = null;

		//starting policy reuse algorithm
		try{
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardHRPRName), true));
			double currTemp = Constants.TEMP;
			for(int k=0; k<numEpisodes; k++){
				//System.out.print(k+" ");
				//choosing an action policy, giving each a probability based on the temperature parameter and the gain W
				double[] probForPolicies = getProbForPolicies(library, currTemp);
				
				if(k%Constants.NUM_EPISODES_PRUNING == 0 && k != 0){
					Tools.printArray(probForPolicies);
					List<Policy> libraryNew = new ArrayList<Policy>();
					libraryNew.add(library.get(0)); //always add current policy
					//prune policies with less weight
					for(int num=1; num<library.size(); num++){ //cannot remove policy 0 (current policy)
						if(probForPolicies[num] > Constants.PRUNING_THRESHOLD){
							libraryNew.add(library.get(num));
						}
					}
					library = new PolicyLibrary(libraryNew);
				}
				
				probForPolicies = getProbForPolicies(library, currTemp);
				probForPolicies = getAccumulatedArray(probForPolicies);
				
				int policyNum = 0;
				if(withHuman){
					//policyNum = 0; //if working with the human, use the current policy
					double maxWeight = Integer.MIN_VALUE;
					policyNum = -1;
					for(int i=0; i<library.size(); i++){
						if(library.get(i).weight > maxWeight){
							maxWeight = library.get(i).weight;
							policyNum = i;
						}
					}
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
				if(isPastPolicy(library, policyNum)){
					//System.out.println("using policy num "+policyNum);
					Policy currPolicy = library.get(policyNum);
					Tuple<Double, Integer, Long> tuple = run(currPolicy, /*true, */Constants.NUM_STEPS_PER_EPISODE);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				} else {
					//System.out.println("using curr policy");
					Tuple<Double, Integer, Long> tuple = run(null, /*true,*/ Constants.NUM_STEPS_PER_EPISODE);
					reward = tuple.getFirst();
					iterations = tuple.getSecond();
					duration = tuple.getThird();
				}

				if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(reward, iterations, duration);
					else{
						if(myWorld.typeOfWorld == Constants.TESTING)
							rewardWriter.write(""+reward+", ");
					}
				}
	           
				Policy currPolicy = library.get(policyNum);
				currPolicy.weight = (currPolicy.weight*currPolicy.numEpisodesChosen + reward)/(currPolicy.numEpisodesChosen + 1);
				currPolicy.numEpisodesChosen = currPolicy.numEpisodesChosen + 1;
				currTemp = currTemp + Constants.DELTA_TEMP;
				
				System.out.println("weights: ");
				library.printWeights();
				System.out.println("num of episodes chosen: ");
				library.printNumEpisodesChosen();
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
		} catch(Exception e){
			e.printStackTrace();
		}	
		return policy;
	}
	
	/**
	 * An array of probabilities is calculated for the policies so that one can be chosen based on the weights associated with each
	 */
	public double[] getProbForPolicies(PolicyLibrary library, double temp) {
		double[] probForPolicies = new double[library.size()];
		double sum = 0;
		for(int i=0; i<probForPolicies.length; i++){
			probForPolicies[i] = Math.pow(Math.E, temp*library.get(i).weight);
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
	
	public boolean isPastPolicy(PolicyLibrary library, int index) {
		return index > 0; //the first (index 0) policy is the current policy
	}
}
