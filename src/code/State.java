package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public int[] stateOfParts;
	
	public State(int[] stateOfParts){
		this.stateOfParts = stateOfParts.clone();
	}
	
	public int getId(){
		int id = 0;
		for(int i=0; i<stateOfParts.length; i++)
			id += Math.pow(Constants.STATES_PER_PART, i)*stateOfParts[i];
		return id;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
	 */
	public String getArduinoString(){
		String str = "";
		for(int i=0; i<stateOfParts.length; i++){
			if(stateOfParts[i] == Constants.NONE || stateOfParts[i] == Constants.COMPLETE)
				str += "0";
			else
				str += ""+stateOfParts[i];
		}
		return str;
	}
	
	public int hashCode() {
		return 5;
	}
	
	public boolean equals(Object Obj){
		State state = (State)Obj;
		for(int i = 0; i < stateOfParts.length; i++) {
			if(stateOfParts[i] != state.stateOfParts[i])
				return false;  
		}
		return true;
	}
	
	public int getNumItemsInState(int stateOfItem){
		int count = 0;
		for(int i=0; i<stateOfParts.length; i++){
			if(stateOfParts[i] == stateOfItem)
				count++;
		}
		return count;
	}
	
	public State clone(){
		return new State(stateOfParts.clone());
	}
	
	public boolean anyItemInState(int stateOfItem){
		for(int i=0; i<stateOfParts.length; i++){
			if(stateOfParts[i] == stateOfItem)
				return true;
		}
		return false;
	}
	
	public boolean allItemsInState(int stateOfItem1){
		for(int i=0; i<stateOfParts.length; i++){
			if(stateOfParts[i] != stateOfItem1)
				return false;
		}
		return true;
	}
	
	public boolean noItemsInState(int stateOfItem){
		for(int i=0; i<stateOfParts.length; i++){
			if(stateOfParts[i] == stateOfItem)
				return false;
		}
		return true;
	}
	
	public String toStringFile() {
		String str = "";
		for(int i=0; i<stateOfParts.length; i++){
			str+=stateOfParts[i];
		}
		return str;
	}
	
	public String toString() {
		String str = "Intensities: ";
		for(int i=0; i<stateOfParts.length; i++){
			if(i == stateOfParts.length-1)
				str+=getCharFromIntensity(stateOfParts[i]);
			else
				str+=getCharFromIntensity(stateOfParts[i])+" ";
		}
		return str;
	}
	
	public String toStringSimple() {
		String str = "";
		for(int i=0; i<stateOfParts.length; i++){
			if(i == stateOfParts.length-1)
				str+=getCharFromIntensity(stateOfParts[i]);
			else
				str+=getCharFromIntensity(stateOfParts[i])+" ";
		}
		return str;
	}
	
	public String getCharFromIntensity(int intensity){
		//if(intensity==Constants.BURNOUT)
		//	return "#";
		return intensity+"";
			
	}
}