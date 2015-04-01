package code;

/**
 * Set of possible actions
 */
public enum Action {
	/*//S_D_i is placing a small drawer in the ith location 
	S_D_0, S_D_1, S_D_2, S_D_3, S_D_4, S_D_5,
	
	//B_D_i is placing a big drawer in the ith location and either i-1th or i+1th location (big drawers take up two horizontal adjacent positions)
	B_D_0, B_D_1, B_D_2, B_D_3, B_D_4, B_D_5, 
	
	//S_C_i is placing a small cabinet in the ith location and either i-2th or i+2th location (small cabinets take up two vertical adjacent positions)
	S_C_0, S_C_1, S_C_2, S_C_3, S_C_4, S_C_5,

	//B_C_i is placing a big cabinet in the ith location and all locations in that column (big cabinets take up all three vertical adjacent positions)
	B_C_0, B_C_1, B_C_2, B_C_3, B_C_4, B_C_5,
	
	WAIT*/
	
	PAINT, LEFT, RIGHT, CLOCKWISE, COUNTER_CLOCKWISE, WAIT
}