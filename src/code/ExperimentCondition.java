package code;

/**
 * An enum specifying the current experimental condition:
 * 
 * -HR_PERTURB: perturbation training using HR-Perturb
 * -PROCE_Q: procedural training using Q-learning
 * -PERTURB_Q: perturbation training using Q-learning
 * -PRQL: policy reuse in Q-learning (used for comparison with HR-Perturb to see if our algorithmic modifications to PRQL are helpful)
 */
public enum ExperimentCondition {
	HR_PERTURB, PROCE_Q, PERTURB_Q, PRQL
}
