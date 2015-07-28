package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.Timer;

/**
 * Implementation of the q learning algorithm
 * Used to learn policies during training
 */
public class QLearner extends LearningAlgorithm {
	public QLearner(QValuesSet qValuesSet, ExperimentCondition condition) {
		this.condition = condition;
		timer = new Timer(1000, timerListener());
		
		if(qValuesSet != null) //transfer the previously learned Q-values if not null
			currQValues = qValuesSet.clone();			
		else //if there are no Q-values to transfer from previous tasks, use the ones from offline learning
			currQValues = new QValuesSet(Main.robotQValuesOffline, Main.jointQValuesOffline);
	}
	
	/**
	 * Run Q-learning for the MDP specified by MyWorld, initialStateHuman is null so the initial state will be randomly selected
	 */
	public void runQLearning(MyWorld myWorld, boolean withHuman){
		runQLearning(myWorld, withHuman, null);
	}
	
	/**
	 * Run Q-learning for the MDP specified by MyWorld and use initialStateHuman as the initial state
	 */
	public void runQLearning(MyWorld myWorld, boolean withHuman, State initialStateHuman) {
		this.myWorld = myWorld;
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
	        for(int k = 0; k < numEpisodes; k++) {
	        	//run one episode of the task
				Tuple<Double, Integer, Long> tuple = run(Constants.NUM_STEPS_PER_EPISODE, initialStateHuman, k, null);
				
				if(Main.SUB_EXECUTION == Main.REWARD_OVER_ITERS){
					if(myWorld.typeOfWorld == Constants.TESTING && k%Constants.INTERVAL == 0)
						Main.rewardOverTime[condition.ordinal()][myWorld.sessionNum-1][(k/Constants.INTERVAL)] += tuple.getFirst(); //tuple.getFirst() == reward
				} else {
		            if(withHuman && Main.saveToFile){
						if(Main.CURRENT_EXECUTION != Main.SIMULATION)
							saveDataToFile(tuple.getFirst(), tuple.getSecond(), tuple.getThird());
						else {
							//if running simulation runs, save the reward into the appropriate file depending on the condition being run
							if(myWorld.typeOfWorld == Constants.TESTING){
								writeToFile(Constants.rewardLimitedTimeData, ""+tuple.getFirst()+",");
								Main.rewardLimitedTime[condition.ordinal()][myWorld.sessionNum-1] += tuple.getFirst();
							}
						}
					}
				}
	        }
		} catch(Exception e){
			e.printStackTrace();
		}
    }
	
	/**
	 * This function saves the current Q-values (robot and joint Q-values) to a file so that the robot can read in the values and start with base knowledge when working with a human
	 */
	public void saveOfflineLearning() {
		try{
			BufferedWriter jointWriter = new BufferedWriter(new FileWriter(new File(Constants.jointQValuesFile), true));
			BufferedWriter robotWriter = new BufferedWriter(new FileWriter(new File(Constants.robotQValuesFile), true));
	
			//saves the robot value function Q(s, a_r) into robotQValuesFile and the joint value function Q(s, a_h, a_r) into jointQValuesFile
	    	for(int i=0; i<MyWorld.states.size(); i++){
				State state = MyWorld.states.get(i);
				for(Action robotAction : Action.values()){
					double robotValue = currQValues.robotQValues[state.getId()][robotAction.ordinal()];
					robotWriter.write(robotValue+",");
					for(Action humanAction : Action.values()){
						double jointValue = currQValues.jointQValues[state.getId()][humanAction.ordinal()][robotAction.ordinal()];
						jointWriter.write(jointValue+",");
					}
				}
			}
	        jointWriter.close();
	        robotWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
