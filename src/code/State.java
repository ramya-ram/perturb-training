package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	//the state contains the human's and robot's locations on the grid
	public Location humanLoc;
	public Location robotLoc;
	
	public State(){
		this.humanLoc = new Location(-1,-1);
		this.robotLoc = new Location(-1,-1);
	}
	
	public State(Location humanLoc, Location robotLoc){
		this.humanLoc = humanLoc.clone();
		this.robotLoc = robotLoc.clone();
	}
	
	/**
	 * Assigns this state to a unique ID so that the value function can refer to states using their unique numbers
	 */
	public int getId(){
		int id = 0;
		int rows = Constants.NUM_ROWS;
		int cols = Constants.NUM_COLS;
		id += humanLoc.row + rows*robotLoc.row + Math.pow(rows, 2)*humanLoc.col + Math.pow(rows, 2)*cols*robotLoc.col;
		return id;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
	 * Since we do not conduct human experiments for this task, this method is empty
	 */
	public String getArduinoString(){
		return "";
	}
	
	public int hashCode() {
		return 5;
	}
	
	/**
	 * Two states are equal if the human's and robot's locations match
	 */
	public boolean equals(Object Obj){
		State state = (State)Obj;
		return (humanLoc.equals(state.humanLoc)) && (robotLoc.equals(state.robotLoc));
	}
	
	public State clone(){
		return new State(humanLoc.clone(), robotLoc.clone());
	}
	
	public String toString() {
		return "H: "+humanLoc+" R: "+robotLoc; 
	}
	
	/**
	 * Returns an array of this state's features (in this case, the array has the format [2,1,9,0] to represent the human and robot locations
	 * The first two numbers are the human location row and column respectively and the second two numbers are the robot's row and column
	 * This is used for sampling data points that can then be inputed into an RBM
	 */
	public int[] toArrayRBM(){
		int[] stateArray = new int[4];
		stateArray[0] = humanLoc.row;
		stateArray[1] = humanLoc.col;
		stateArray[2] = robotLoc.row;
		stateArray[3] = robotLoc.col;
		return stateArray;
	}
	
	/**
	 * Writes the state into a file using a format like "2,1,9,0" to represent the human and robot locations
	 * The first two numbers are the human location row and column respectively and the second two numbers are the robot's row and column
	 * This is used for sampling data points that can then be inputed into an RBM
	 */
	public String toStringRBM(){
		return humanLoc.row+","+humanLoc.col+","+robotLoc.row+","+robotLoc.col;
	}
}