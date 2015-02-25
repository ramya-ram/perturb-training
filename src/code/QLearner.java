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

	public QLearner(QValuesSet qValuesSet, ExperimentCondition condition) {
		this.condition = condition;
		timer = new Timer(1000, timerListener());
		
		//if there are no qvalues to transfer from previous tasks, use the ones from offline learning
		if(qValuesSet == null){
			currQValues = new QValuesSet(Main.robotQValuesOffline, Main.jointQValuesOffline);
		} else { //otherwise, transfer the previously learned q-values
			currQValues = qValuesSet.clone();
		}
	}
	
	public void run(MyWorld myWorld, boolean withHuman){
		run(myWorld, withHuman, null);
	}
	
	/**
	 * Run QLearning for the number of episodes specified and see how accumulated reward changes over these episodes
	 */
	public void run(MyWorld myWorld, boolean withHuman, State initialStateHuman) {
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
		System.out.println("testWind="+myWorld.testWind+" testDryness="+myWorld.testDryness+" simulationWind="+myWorld.simulationWind+" simulationDryness="+myWorld.simulationDryness);
		
		if(withHuman && Main.gameView != null){
			System.out.println("with human");
			Main.gameView.setStartRoundEnable(true);
			Main.gameView.waitForStartRoundClick();
		}
		
		try{
	        for(int i = 0; i < numEpisodes; i++) {
				Tuple<Double, Integer, Long> tuple = run(Constants.NUM_STEPS_PER_EPISODE, initialStateHuman);
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
    }
	
	public void saveOfflineLearning() {
		try{
			BufferedWriter jointWriter = new BufferedWriter(new FileWriter(new File(Constants.jointQValuesFile), true));
			BufferedWriter robotWriter = new BufferedWriter(new FileWriter(new File(Constants.robotQValuesFile), true));
	
	    	int num=0;
			int statesPerFire = Constants.STATES_PER_FIRE;
	        for(int i=0; i<statesPerFire; i++){
				for(int j=0; j<statesPerFire; j++){
					for(int k=0; k<statesPerFire; k++){
						for(int l=0; l<statesPerFire; l++){
							for(int m=0; m<statesPerFire; m++){
								int[] stateOfFires = {i,j,k,l,m};
								State state = new State(stateOfFires);
								for(Action robotAction : Action.values()){
									double robotValue = currQValues.robotQValues[state.getId()][robotAction.ordinal()];
									robotWriter.write(robotValue+",");
									for(Action humanAction : Action.values()){
										num++;
										double jointValue = currQValues.jointQValues[state.getId()][humanAction.ordinal()][robotAction.ordinal()];
										jointWriter.write(jointValue+",");
									}
								}
							}
						}
					}
				}
			}
	        
	        System.out.println("num "+num);
	        jointWriter.close();
	        robotWriter.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
