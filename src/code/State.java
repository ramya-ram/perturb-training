package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public int[] stateOfBox;
	public int humanPos;
	public int robotPos;
	public int robotOrientation;
	
	public State(int[] stateOfBox, int humanPos, int robotPos, int robotOrientation){
		this.stateOfBox = stateOfBox.clone();
		this.humanPos = humanPos;
		this.robotPos = robotPos;
		this.robotOrientation = robotOrientation;
	}
	
	public int getId(){
		int statesPerItem = Constants.STATES_PER_ITEM;
		double exp = Math.pow(statesPerItem, stateOfBox.length);
		int id = 0;
		for(int i=0; i<stateOfBox.length; i++)
			id += Math.pow(statesPerItem, i)*stateOfBox[i];
		id +=  exp*humanPos + exp*Constants.NUM_POS*robotPos + exp*Constants.NUM_POS*Constants.NUM_POS*robotOrientation;
		return id;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
	 */
	public String getArduinoString(){
		String str = "";
		for(int i=0; i<stateOfBox.length; i++){
			//if(stateOfBox[i] == Constants.NONE || stateOfBox[i] == Constants.BURNOUT)
			//	str += "0";
			//else
				str += ""+stateOfBox[i];
		}
		return str;
	}
	
	public int hashCode() {
		return 5;
	}
	
	public boolean equals(Object Obj){
		State state = (State)Obj;
		if(humanPos != state.humanPos)
			return false;
		if(robotPos != state.robotPos)
			return false;
		if(robotOrientation != state.robotOrientation)
			return false;
		for(int i = 0; i < stateOfBox.length; i++) {
			if(stateOfBox[i] != state.stateOfBox[i])
				return false;  
		}
		return true;
	}
	
	public int getNumItemsInState(int stateOfItem){
		int count = 0;
		for(int i=0; i<stateOfBox.length; i++){
			if(stateOfBox[i] == stateOfItem)
				count++;
		}
		return count;
	}
	
	public State clone(){
		return new State(stateOfBox.clone(), humanPos, robotPos, robotOrientation);
	}
	
	public boolean anyItemInState(int stateOfItem){
		for(int i=0; i<stateOfBox.length; i++){
			if(stateOfBox[i] == stateOfItem)
				return true;
		}
		return false;
	}
	
	public boolean allItemsInState(int stateOfItem1){
		for(int i=0; i<stateOfBox.length; i++){
			if(stateOfBox[i] != stateOfItem1)
				return false;
		}
		return true;
	}
	
	public boolean noItemsInState(int stateOfItem){
		for(int i=0; i<stateOfBox.length; i++){
			if(stateOfBox[i] == stateOfItem)
				return false;
		}
		return true;
	}
	
	public String toStringFile() {
		String str = "";
		for(int i=0; i<stateOfBox.length; i++){
			str+=stateOfBox[i];
		}
		str+= ","+humanPos+","+robotPos+","+robotOrientation;
		return str;
	}
	
	public String toString() {
		String str = "";
		for(int i=0; i<stateOfBox.length; i++){
			if(i == stateOfBox.length-1)
				str+=getCharFromIntensity(stateOfBox[i]);
			else
				str+=getCharFromIntensity(stateOfBox[i])+" ";
		}
		str+=" HumanPos: "+humanPos+" RobotPos: "+robotPos+" RobotOrient: "+robotOrientation;
		return str;
	}
	
	public String toStringSimple() {
		String str = "";
		for(int i=0; i<stateOfBox.length; i++){
			if(i == stateOfBox.length-1)
				str+=getCharFromIntensity(stateOfBox[i]);
			else
				str+=getCharFromIntensity(stateOfBox[i])+" ";
		}
		str+= humanPos+", "+robotPos+", "+robotOrientation;
		return str;
	}
	
	public String getCharFromIntensity(int intensity){
		//if(intensity==Constants.BURNOUT)
			//return "#";
		return intensity+"";
			
	}
}