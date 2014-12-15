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
	public ExperimentCondition condition;
	public QLearner(QValuesSet qValuesSet, boolean useFileName, ExperimentCondition condition) {
		this.condition = condition;
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
		
		//System.out.println("myWorld typeOfWorld "+myWorld.typeOfWorld+" sessionNum "+myWorld.sessionNum+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness+" testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness);
		
		System.out.println("wind="+myWorld.testWind+" dryness="+myWorld.testDryness);
		
		if(withHuman && Main.gameView != null){
			System.out.println("with human");
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		Policy policy = null;
		
		try{
	        for(int i = 0; i < numEpisodes; i++) {
				Tuple<Double, Integer, Long> tuple = run(null/*, false egreedy*/, Constants.NUM_STEPS_PER_EPISODE);
				//System.out.print(i+" ");
	            
	            if(withHuman && Main.saveToFile){
					if(Main.CURRENT_EXECUTION != Main.SIMULATION)
						saveDataToFile(tuple.getFirst(), tuple.getSecond(), tuple.getThird());
					else{
						if(myWorld.typeOfWorld == Constants.TESTING){
							String fileName = "";
							if(condition == ExperimentCondition.PERTURB_Q)
			    				fileName = Constants.rewardPerturbQName;
			    			else if(condition == ExperimentCondition.PROCE_Q)
			    				fileName = Constants.rewardProceQName;
							BufferedWriter rewardWriter = new BufferedWriter(new FileWriter(new File(fileName), true));

							System.out.println("writing");
							rewardWriter.write(""+tuple.getFirst()+", ");
							rewardWriter.close();
						}
					}
				}
	        }
			if(computePolicy)
				policy = computePolicy();
			long end = System.currentTimeMillis();
			if(myWorld.typeOfWorld == Constants.TESTING && !withHuman){
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(Constants.simulationDir+"duration"+Constants.NUM_EPISODES_TEST+".csv"), true));
				System.out.println("qlearner duration "+(end-start));
				writer.write((end-start)+"\n");
				writer.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return policy;
    }
}
