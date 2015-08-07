package code;

/**
 * An enum specifying the current experimental condition:
 * 
 * -ADAPT: perturbation training using ADAPT
 * -PRQL: perturbation training using Policy Reuse in Q-learning--PRQL
 * -PRQL_RBM: perturbation training using PRQL + RBDist as a MDP similarity metric to choose the prior
 * -Q_LEARNING: perturbation training using standard Q-learning from scratch with no prior knowledge
 * -PROCE_Q: procedural training using Q-learning
 * -PERTURB_Q: perturbation training using Q-learning
 */
public enum ExperimentCondition {
	ADAPT, PRQL, PRQL_RBM, Q_LEARNING, PROCE_Q, PERTURB_Q
}
