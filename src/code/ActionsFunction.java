package code;

import java.util.Set;

/**
 * An interface used to specify the set of possible actions that can be taken from each state
 */
public interface ActionsFunction {
	public Set<Action> actions(State s);
}
