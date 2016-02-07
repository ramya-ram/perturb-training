package code;

/**
 * Represents a pair object that can be used to return two values in a method
 */
public class Pair<First, Second> {

	private First first;
	private Second second;

	public Pair(First first, Second second) {
		this.first = first;
		this.second = second;
	}

	public First getFirst() {
		return first;
	}

	public Second getSecond() {
		return second;
	}

	public void setFirst(First first) {
		this.first = first;
	}

	public void setSecond(Second second) {
		this.second = second;
	}
	
	public String toString(){
		return first+" "+second;
	}
}