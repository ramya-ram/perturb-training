package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public Location humanLoc;
	public Location robotLoc;
	public int humanItem;
	public int robotItem;
	//public int obstacleRow;
	
	public State(Location humanLoc, Location robotLoc, int humanItem, int robotItem){//, int obstacleRow){
		this.humanLoc = humanLoc.clone();
		this.robotLoc = robotLoc.clone();
		this.humanItem = humanItem;
		this.robotItem = robotItem;
		//this.obstacleRow = obstacleRow;
	}
	
	public int getId(){
		int id = 0;
		int rows = Constants.NUM_ROWS;
		int cols = Constants.NUM_COLS;
		int numItems = Constants.NUM_ITEMS+1; //one for each items and one for no item = NUM_ITEMS + 1
		double squaredSum = Math.pow(rows, 2)*Math.pow(cols, 2);
		//for(int i=0; i<stateOfFires.length; i++)
		//	id += Math.pow(Constants.STATES_PER_FIRE, i)*stateOfFires[i];
		
		id += humanLoc.row + rows*robotLoc.row + Math.pow(rows, 2)*humanLoc.col + Math.pow(rows, 2)*cols*robotLoc.col + squaredSum*(humanItem+1) + 
				squaredSum*numItems*(robotItem+1);// +  squaredSum*Math.pow(numItems, 2)*obstacleRow;
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
		return (humanLoc.equals(state.humanLoc)) && (robotLoc.equals(state.robotLoc) && (humanItem == state.humanItem) && (robotItem == state.robotItem));// && (obstacleRow == state.obstacleRow));
	}
	
	public State clone(){
		return new State(humanLoc.clone(), robotLoc.clone(), humanItem, robotItem);//, obstacleRow);
	}
	
	public String toString() {
		return "H: "+humanLoc+", "+humanItem+" R: "+robotLoc+", "+robotItem;//+" Obstacle: "+obstacleRow; 
	}
	
	public String getCharFromIntensity(int intensity){
		//if(intensity==Constants.BURNOUT)
		//	return "#";
		return intensity+"";
			
	}
}