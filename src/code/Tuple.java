package code;

/**
 * Represents a tuple object that can be used to return three values in a method
 */
public class Tuple<First, Second, Third> {
	private First first;
	private Second second;
	private Third third;

	public Tuple(First first, Second second, Third third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public First getFirst() {
		return first;
	}

	public Second getSecond() {
		return second;
	}

	public Third getThird() {
		return third;
	}

	public void setFirst(First first) {
		this.first = first;
	}

	public void setSecond(Second second) {
		this.second = second;
	}

	public void setThird(Third third) {
		this.third = third;
	}
}