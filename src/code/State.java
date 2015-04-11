package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public Location humanLoc;
	public Location robotLoc;
	
	public State(Location humanLoc, Location robotLoc){
		this.humanLoc = humanLoc.clone();
		this.robotLoc = robotLoc.clone();
	}
	
	public int getId(){
		int id = 0;
		int rows = Constants.NUM_ROWS;
		int cols = Constants.NUM_COLS;
		//for(int i=0; i<stateOfFires.length; i++)
		//	id += Math.pow(Constants.STATES_PER_FIRE, i)*stateOfFires[i];
		id += humanLoc.row + rows*robotLoc.row + Math.pow(rows, 2)*humanLoc.col + Math.pow(rows, 2)*cols*robotLoc.col;
		return id;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
	 */
	public String getArduinoString(){
		String str = "";
		return str;
	}
	
	public int hashCode() {
		return 5;
	}
	
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
	
	public String getCharFromIntensity(int intensity){
		//if(intensity==Constants.BURNOUT)
		//	return "#";
		return intensity+"";
			
	}
}