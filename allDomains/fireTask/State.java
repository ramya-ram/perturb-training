package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public int[] stateOfFires;
	
	public State(){
		this.stateOfFires = new int[Constants.NUM_FIRES];
	}
	
	public State(int[] stateOfFires){
		this.stateOfFires = stateOfFires.clone();
	}
	
	public int getId(){
		int id = 0;
		for(int i=0; i<stateOfFires.length; i++)
			id += Math.pow(Constants.STATES_PER_FIRE, i)*stateOfFires[i];
		return id;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
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
	
	public int hashCode() {
		return 5;
	}
	
	public boolean equals(Object Obj){
		State state = (State)Obj;
		for(int i = 0; i < stateOfFires.length; i++) {
			if(stateOfFires[i] != state.stateOfFires[i])
				return false;  
		}
		return true;
	}
	
	public int getNumItemsInState(int stateOfItem){
		int count = 0;
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == stateOfItem)
				count++;
		}
		return count;
	}
	
	public State clone(){
		return new State(stateOfFires.clone());
	}
	
	public boolean anyItemInState(int stateOfItem){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == stateOfItem)
				return true;
		}
		return false;
	}
	
	public boolean allItemsInState(int stateOfItem1, int stateOfItem2){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] != stateOfItem1 && stateOfFires[i] != stateOfItem2)
				return false;
		}
		return true;
	}
	
	public boolean noItemsInState(int stateOfItem){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == stateOfItem)
				return false;
		}
		return true;
	}
	
	public String toStringFile() {
		String str = "";
		for(int i=0; i<stateOfFires.length; i++){
			str+=stateOfFires[i];
		}
		return str;
	}
	
	public String toString() {
		String str = "Intensities: ";
		for(int i=0; i<stateOfFires.length; i++){
			if(i == stateOfFires.length-1)
				str+=getCharFromIntensity(stateOfFires[i]);
			else
				str+=getCharFromIntensity(stateOfFires[i])+" ";
		}
		return str;
	}
	
	public String toStringSimple() {
		String str = "";
		for(int i=0; i<stateOfFires.length; i++){
			if(i == stateOfFires.length-1)
				str+=getCharFromIntensity(stateOfFires[i]);
			else
				str+=getCharFromIntensity(stateOfFires[i])+" ";
		}
		return str;
	}
	
	public String getCharFromIntensity(int intensity){
		if(intensity==Constants.BURNOUT)
			return "#";
		return intensity+"";
			
	}
}