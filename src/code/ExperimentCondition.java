package code;

/**
 * An enum specifying the current experimental condition:
 * 
 * -ADAPT: perturbation training using ADAPT
 * -PROCE_Q: procedural training using Q-learning
 * -PERTURB_Q: perturbation training using Q-learning
 * -PRQL: policy reuse in Q-learning (used for comparison with AdaPT to see if our algorithmic modifications to PRQL are helpful)
 * -Q_LEARNING: standard Q-learning from scratch with no prior knowledge
 */
public enum ExperimentCondition {
	ADAPT, PRQL, PRQL_RBM, Q_LEARNING, PROCE_Q, PERTURB_Q
}
