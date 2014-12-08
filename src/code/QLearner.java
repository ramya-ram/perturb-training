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

	public QLearner(QValuesSet qValuesSet, boolean useFileName) {
		timer = new Timer(1000, timerListener());
		samples = new int[Constants.NUM_SAMPLES][Constants.NUM_FEATURES];
		
		//if there are no qvalues to transfer from previous tasks, initialize the q-value table
		if(qValuesSet == null){
			robotQValues = new double[MyWorld.mdp.states.size()][Action.values().length];
			jointQValues = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
		} else { //otherwise, transfer the previously learned q-values
			robotQValues = qValuesSet.getRobotQValues();
			jointQValues = qValuesSet.getJointQValues();
		}
		//q-values learned offline are transferred
		if(useFileName){		
			robotQValues = new double[MyWorld.mdp.states.size()][Action.values().length];
			jointQValues = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
			for(int i=0; i<jointQValues.length; i++){
				for(int j=0; j<jointQValues[i].length; j++){
					robotQValues[i][j] = Main.robotQValuesOffline[i][j];
					for(int k=0; k<jointQValues[i][j].length; k++){
						jointQValues[i][j][k] = Main.jointQValuesOffline[i][j][k];
					}
				}
			}
			System.out.println("using offline values");
		}
	}
	
	/**
	 * Run QLearning for the number of episodes specified and see how accumulated reward changes over these episodes
	 */
	public Policy run(MyWorld myWorld, boolean withHuman, boolean computePolicy) {
		this.myWorld = myWorld;
		this.mdp = MyWorld.mdp;
		this.withHuman = withHuman;
		Main.currWithSimulatedHuman = withHuman;
		/*if(withHuman && Main.CURRENT_EXECUTION == Main.SIMULATION){
			if(computePolicy)
				return computePolicy();
			return null;
		}*/
		myWorld.setWindAndDryness();
//		if(withHuman){
//			this.epsilon = Main.HUMAN_EPSILON;
//			//Main.humanInteractionNum++;
//		} else {
//			this.epsilon = Main.SIMULATION_EPSILON;
//		}
		numCurrentSamples = 0;
		
		int numEpisodes = Constants.NUM_EPISODES;
		if(myWorld.typeOfWorld == Constants.TESTING){
			currCommunicator = Constants.ROBOT; //robot initiates
			numEpisodes = Constants.NUM_EPISODES_TEST;
		}
		if(withHuman)
			numEpisodes = 1;
		
		resetCommunicationCounts();
		
		System.out.println("myWorld typeOfWorld "+myWorld.typeOfWorld+" sessionNum "+myWorld.sessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		if(withHuman && Main.gameView != null){
			System.out.println("with human");
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		
		try{
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardProceName), true));
	        for(int i = 0; i < numEpisodes; i++) {
				Tuple<Double, Integer, Long> tuple = run(false /*egreedy*/, Constants.NUM_STEPS_PER_EPISODE);
	            
	            if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(tuple.getFirst(), tuple.getSecond(), tuple.getThird());
					else{
						if(myWorld.typeOfWorld == Constants.TESTING){
							System.out.println("writing");
							rewardWriter.write(""+tuple.getFirst()+", ");
						}
					}
				}
	        }
	        rewardWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		if(computePolicy)
			return computePolicy();
		return null;
    }
}
