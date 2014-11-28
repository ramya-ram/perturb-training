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

	public QLearner(SocketConnect connect, QValuesSet qValuesSet, boolean useFileName) {
		this.connect = connect;
		timer = new Timer(1000, timerListener());
		
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
		Main.currWithSimulatedHuman = withHuman;
//		if(withHuman){
//			this.epsilon = Main.HUMAN_EPSILON;
//			//Main.humanInteractionNum++;
//		} else {
//			this.epsilon = Main.SIMULATION_EPSILON;
//		}
		if(myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM)
			currCommunicator = 1; //robot initiates
		
		resetCommunicationCounts();
		
		System.out.println("myWorld "+myWorld.trainingSessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		try{
			BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(Constants.rewardProceName), true));
	        for(int i = 0; i < Constants.NUM_EPISODES; i++) {
				Tuple<Double, Integer, Long> tuple = run(false /*egreedy*/, Constants.NUM_STEPS_PER_EPISODE);
	            if(Main.currWithSimulatedHuman && Main.saveToFile && (myWorld.trainingSessionNum == MyWorld.PROCE_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB1_TEST_NUM || myWorld.trainingSessionNum == MyWorld.PERTURB2_TEST_NUM)){
		            rewardWriter.write(""+tuple.getFirst()+", ");
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
