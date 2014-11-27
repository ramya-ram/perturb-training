package code;

/**
 * Representation for a state in this MDP
 */
public class State {
	int[] stateOfFires;
	
	public State(int[] stateOfFires){
		this.stateOfFires = stateOfFires.clone();
	}
	
	public int getId(){
		int id = 0;
		for(int i=0; i<stateOfFires.length; i++){
			id += Math.pow(MyWorld.STATES_PER_FIRE, i)*stateOfFires[i];
		}
		return id;
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
	
	public int sum() {
		int sum = 0;
		for(int i=0; i<stateOfFires.length; i++){
			//if(stateOfFires[i] == MyWorld.BURNOUT)
			//	sum += 10;
			//else
				sum += stateOfFires[i];
		}
		return sum;
	}
	
	public State clone(){
		return new State(stateOfFires.clone());
	}
	
	public State convertBurnoutToNone() {
		State newState = this.clone();
		for(int i=0; i<newState.stateOfFires.length; i++){
			if(newState.stateOfFires[i] == MyWorld.BURNOUT)
				newState.stateOfFires[i] = MyWorld.NONE;
		}
		return newState;
	}
	
	public boolean anyItemInState(int stateOfItem){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] == stateOfItem)
				return true;
		}
		return false;
	}
	
	public boolean allItemsInState(int stateOfItem){
		for(int i=0; i<stateOfFires.length; i++){
			if(stateOfFires[i] != stateOfItem)
				return false;
		}
		return true;
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
		if(intensity==MyWorld.BURNOUT)
			return "#";
		return intensity+"";
			
	}
}