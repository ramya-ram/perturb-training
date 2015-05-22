package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public Location humanLoc;
	public Location robotLoc;
	public int humanItem;
	public int robotItem;
	public Location obstacle;
	
	public State(Location humanLoc, Location robotLoc, int humanItem, int robotItem, Location obstacle){
		this.humanLoc = humanLoc.clone();
		this.robotLoc = robotLoc.clone();
		this.humanItem = humanItem;
		this.robotItem = robotItem;
		this.obstacle = obstacle.clone();
	}
	
	public int getId(){
		int id = 0;
		int rows = Constants.NUM_ROWS;
		int cols = Constants.NUM_COLS;
		int numItems = Constants.NUM_ITEMS;
		double squaredSum = Math.pow(rows, 2)*Math.pow(cols, 2);
		//for(int i=0; i<stateOfFires.length; i++)
		//	id += Math.pow(Constants.STATES_PER_FIRE, i)*stateOfFires[i];
		
		id += humanLoc.row + rows*robotLoc.row + Math.pow(rows, 2)*humanLoc.col + Math.pow(rows, 2)*cols*robotLoc.col + squaredSum*humanItem + 
				squaredSum*numItems*robotItem +  squaredSum*Math.pow(numItems, 2)*obstacle.row;
		//id += humanLoc.row + rows*robotLoc.row + Math.pow(rows, 2)*humanLoc.col + Math.pow(rows, 2)*cols*robotLoc.col + squaredSum*humanItem + 
		//		squaredSum*numItems*robotItem +  squaredSum*Math.pow(numItems, 2)*obstacle.row + squaredSum*Math.pow(numItems, 2)*rows*obstacle.col;
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
		return (humanLoc.equals(state.humanLoc)) && (robotLoc.equals(state.robotLoc) && (humanItem == state.humanItem) && (robotItem == state.robotItem) && (obstacle.equals(state.obstacle)));
	}
	
	public State clone(){
		return new State(humanLoc.clone(), robotLoc.clone(), humanItem, robotItem, obstacle.clone());
	}
	
	public String toString() {
		return "H: "+humanLoc+", "+humanItem+" R: "+robotLoc+", "+robotItem+" Obstacle: "+obstacle; 
	}
	
	public String getCharFromIntensity(int intensity){
		//if(intensity==Constants.BURNOUT)
		//	return "#";
		return intensity+"";
			
	}
}