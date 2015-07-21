package code;

/**
 * Specifies a location on the grid (with a row and column)
 */
public class Location{
	public int row;
	public int col;
	
	public Location(int row, int col){
		this.row = row;
		this.col = col;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getCol() {
		return col;
	}

	public boolean equals(Object obj) {
		Location otherLoc = (Location) obj;
		return otherLoc.row == row && otherLoc.col == col;
	}
	
	public String toString() {
		return "("+row+", "+col+") ";
	}
	
	public int hashCode() {
		return 4;
	}
	
	public Location clone(){
		return new Location(row, col);
	}
}