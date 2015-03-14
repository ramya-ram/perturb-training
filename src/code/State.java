package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	public int[] stateOfItems;
	public int humanPos;
	public int robotPos;
	
	public State(int[] stateOfItems, int humanPos, int robotPos){
		this.stateOfItems = stateOfItems.clone();
		this.humanPos = humanPos;
		this.robotPos = robotPos;
	}
	
	public int getId(){
		int statesPerItem = Constants.STATES_PER_ITEM;
		int id = 0;
		for(int i=0; i<stateOfItems.length; i++)
			id += Math.pow(statesPerItem, i)*stateOfItems[i];
		id +=  Math.pow(statesPerItem, stateOfItems.length)*humanPos + Math.pow(statesPerItem, stateOfItems.length)*Constants.NUM_POS*robotPos;
		return id;
	}
	
	/**
	 * This string is sent to the arduino to display the current intensities of the fires on the LED lights
	 */
	public String getArduinoString(){
		String str = "";
		for(int i=0; i<stateOfItems.length; i++){
			//if(stateOfItems[i] == Constants.NONE || stateOfItems[i] == Constants.BURNOUT)
			//	str += "0";
			//else
				str += ""+stateOfItems[i];
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
		for(int i = 0; i < stateOfItems.length; i++) {
			if(stateOfItems[i] != state.stateOfItems[i])
				return false;  
		}
		return true;
	}
	
	public int getNumItemsInState(int stateOfItem){
		int count = 0;
		for(int i=0; i<stateOfItems.length; i++){
			if(stateOfItems[i] == stateOfItem)
				count++;
		}
		return count;
	}
	
	public State clone(){
		return new State(stateOfItems.clone(), humanPos, robotPos);
	}
	
	public boolean anyItemInState(int stateOfItem){
		for(int i=0; i<stateOfItems.length; i++){
			if(stateOfItems[i] == stateOfItem)
				return true;
		}
		return false;
	}
	
	public boolean allItemsInState(int stateOfItem1){
		for(int i=0; i<stateOfItems.length; i++){
			if(stateOfItems[i] != stateOfItem1)
				return false;
		}
		return true;
	}
	
	public boolean noItemsInState(int stateOfItem){
		for(int i=0; i<stateOfItems.length; i++){
			if(stateOfItems[i] == stateOfItem)
				return false;
		}
		return true;
	}
	
	public String toStringFile() {
		String str = "";
		for(int i=0; i<stateOfItems.length; i++){
			str+=stateOfItems[i];
		}
		str+= ","+humanPos+","+robotPos;
		return str;
	}
	
	public String toString() {
		String str = "";
		for(int i=0; i<stateOfItems.length; i++){
			if(i == stateOfItems.length-1)
				str+=getCharFromIntensity(stateOfItems[i]);
			else
				str+=getCharFromIntensity(stateOfItems[i])+" ";
		}
		str+=" Human: "+humanPos+" Robot: "+robotPos;
		return str;
	}
	
	public String toStringSimple() {
		String str = "";
		for(int i=0; i<stateOfItems.length; i++){
			if(i == stateOfItems.length-1)
				str+=getCharFromIntensity(stateOfItems[i]);
			else
				str+=getCharFromIntensity(stateOfItems[i])+" ";
		}
		str+= humanPos+", "+robotPos;
		return str;
	}
	
	public String getCharFromIntensity(int intensity){
		//if(intensity==Constants.BURNOUT)
			//return "#";
		return intensity+"";
			
	}
}