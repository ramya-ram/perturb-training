package code;

/**
 * Used to transfer q values between sessions
 */
public class QValuesSet {
	private double[][] robotQValues; 
	private double[][][] jointQValues;
	
	public QValuesSet(double[][] robotQValues_param, double[][][] jointQValues_param){
		this.robotQValues = new double[MyWorld.mdp.states.size()][Action.values().length];//new HashMap<StateRobotActionPair, Double>();
		this.jointQValues = new double[MyWorld.mdp.states.size()][Action.values().length][Action.values().length];
		for(int i=0; i<this.jointQValues.length; i++){
			for(int j=0; j<this.jointQValues[i].length; j++){
				this.robotQValues[i][j] = robotQValues_param[i][j];
				for(int k=0; k<this.jointQValues[i][j].length; k++){
					this.jointQValues[i][j][k] = jointQValues_param[i][j][k];
				}
			}
		}
	}

	public double[][] getRobotQValues() {
		return robotQValues;
	}

	public double[][][] getJointQValues() {
		return jointQValues;
	}
}
