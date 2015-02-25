package code;

/**
 * Used to store and transfer a Q-values set that includes both the robot Q-values Q(s, a_r) and the joint Q-values Q(s, a_h, a_r)
 */
public class QValuesSet {
	public double[][] robotQValues; 
	public double[][][] jointQValues;
	public double weight;
	public int numEpisodesChosen;
	
	public QValuesSet(){
		this(null, null);
	}
		
	/**
	 * Does a deep copy of the Q-values set (copies every element rather than the references)
	 */
	public QValuesSet(double[][] robotQValues_param, double[][][] jointQValues_param){
		this.robotQValues = new double[MyWorld.mdp.states.size()][Action.values().length];//new HashMap<StateRobotActionPair, Double>();
		this.jointQValues = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
		if(robotQValues_param != null && jointQValues_param != null){
			for(int i=0; i<this.jointQValues.length; i++){
				for(int j=0; j<this.jointQValues[i].length; j++){
					this.robotQValues[i][j] = robotQValues_param[i][j];
					for(int k=0; k<this.jointQValues[i][j].length; k++){
						this.jointQValues[i][j][k] = jointQValues_param[i][j][k];
					}
				}
			}
		}
		weight = 0;
		numEpisodesChosen = 0;
	}

	public double[][] getRobotQValues() {
		return robotQValues;
	}

	public double[][][] getJointQValues() {
		return jointQValues;
	}
	
	public QValuesSet clone(){
		return new QValuesSet(robotQValues, jointQValues);
	}
}
