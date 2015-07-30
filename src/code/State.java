package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	//the state contains the intensity for each fire
	//e.g. if there are 5 fires, the array would be length 5, each value would indicate the intensity of that particular fire
	public int[] stateOfFires;
	
	public State(){
		this.stateOfFires = new int[Constants.NUM_FIRES];
	}
	
	public State(int[] stateOfFires){
		this.stateOfFires = stateOfFires.clone();
	}
	
	/**
	 * Assigns this state to a unique ID so that the value function can refer to states using their unique numbers
	 */
	public int getId(){
		int id = 0;
		for(int i=0; i<stateOfFires.length; i++)
			id += Math.pow(Constants.STATES_PER_FIRE, i)*stateOfFires[i];
		return id;
	}
	
	public int hashCode() {
		return 5;
	}
	
	/**
	 * Two states are equal if all values in the array match (intensities of corresponding fires are equal)
	 */
	public boolean equals(Object Obj){
		State state = (State)Obj;
		for(int i = 0; i < stateOfFires.length; i++) {
			if(stateOfFires[i] != state.stateOfFires[i])
				return false;  
		}
		return true;
	}
	
	public State clone(){
		return new State(stateOfFires.clone());
	}
	
	/**
	 * Returns the number of values in the stateOfFires array that is equal to the given stateOfItem
	 */
	public int getNumItemsInState(int stateOfItem){
		int count = 0;
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == stateOfItem)
				count++;
		}
		return count;
	}
	
	/**
	 * If all values in the array are equal to either of the given stateOfItems, return true
	 */
	public boolean allItemsInState(int stateOfItem1, int stateOfItem2){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] != stateOfItem1 && stateOfFires[i] != stateOfItem2)
				return false;
		}
		return true;
	}
	
	/**
	 * If no value in the array is equal to the given stateOfItem, return true
	 */
	public boolean noItemsInState(int stateOfItem){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == stateOfItem)
				return false;
		}
		return true;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
	 * Both none and burnout have value of 0 (because in both, the light in the experiments will be turned off)
	 */
	public String getArduinoString(){
		String str = "";
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == Constants.NONE || stateOfFires[i] == Constants.BURNOUT)
				str += "0";
			else
				str += ""+stateOfFires[i];
		}
		return str;
	}
	
	/**
	 * Writes the state to a file using a format like "03143" to represent the intensities of the 5 fires, A, B, C, D, and E respectively
	 */
	public String toString() {
		String str = "";
		for(int i=0; i<stateOfFires.length; i++){
			str+=stateOfFires[i];
		}
		return str;
	}
	
	/**
	 * Writes the state on the GUI using a format like "0 3 1 # 3" to represent the intensities of the 5 fires, A, B, C, D, and E respectively
	 */
	public String toStringGUI() {
		String str = "";
		for(int i=0; i<stateOfFires.length; i++){
			if(i == stateOfFires.length-1)
				str+=getCharFromIntensity(stateOfFires[i]);
			else
				str+=getCharFromIntensity(stateOfFires[i])+" ";
		}
		return str;
	}
	
	/**
	 * Returns an array of this state's features (in this case, it's just the state of the fires)
	 * This is used for sampling data points that can then be inputed into an RBM
	 */
	public int[] toArrayRBM(){
		return stateOfFires;
	}
	
	/**
	 * Writes the state into a file using a format like "0,3,1,4,3" to represent the intensities of the 5 fires, A, B, C, D, and E respectively
	 * This is used for sampling data points that can then be inputed into an RBM
	 */
	public String toStringRBM(){
		String str = "";
		for(int i=0; i<stateOfFires.length; i++){
			if(i == stateOfFires.length-1)
				str+=stateOfFires[i];
			else
				str+=stateOfFires[i]+",";
		}
		return str;
	}
	
	/**
	 * Replaces the character '#' for fires that are burned out
	 */
	public String getCharFromIntensity(int intensity){
		if(intensity==Constants.BURNOUT)
			return "#";
		return intensity+"";
			
	}
}