package code;

import java.util.ArrayList;
import java.util.List;

/**
 * An agent (robot or human)
 * Has an action function that defines what the agent can do at each state
 */
public abstract class Agent {
	public ActionsFunction actionsFunction;

	/**
	 * Returns the possible actions that can be taken from state s as an array
	 */
	public Action[] actions(State s) {
		List<Action> actionsList = new ArrayList<Action>();
		actionsList.addAll(actionsFunction.actions(s));
		Action[] actionsArr = new Action[actionsList.size()];
		for(int i=0; i<actionsList.size(); i++)
			if(actionsList.get(i) instanceof Action)
				actionsArr[i] = (Action)actionsList.get(i);
		return actionsArr;
	}
	
	/**
	 * Returns the possible actions that can be taken from state s as an array
	 */
	public ArrayList<Action> actionsAsList(State s) {
		ArrayList<Action> actionsList = new ArrayList<Action>();
		actionsList.addAll(actionsFunction.actions(s));
		return actionsList;
	}
}
