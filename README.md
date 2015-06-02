# README #

This README explains how to use this code to run human subject experiments and computational simulations.

### What is this repository for? ###

* This repository holds the code for human-robot perturbation training.
* It can be used to run human subject experiments with a simulated robot and with an embodied robot (the PR2).
* It can also be used to run computational simulations without a human over many domains.

### How do I get set up? ###

* Summary of set up:
	Make sure you download the Java code base, which should have source files as well as input files (predefined cases, offline learning values) and data (pictures used for the GUI).
	
	For running robot experiments, you will need an Ubuntu machine with ROS installed (I use VMWare Workstation to have an Ubuntu virtual box on my Windows machine)
	You will also need the Arduino environment installed to control the lights using the code (represents different intensities of fires)
	Finally, you will need Google Web Speech Recognition: go to http://www.google.com/intl/en/chrome/demos/speech.html and 
	Download the ROS code and the Arduino hardware code from the code base.
	To capture human speech input, download the google web speech recognition code.

	You will need to add to add some external jars to your project: RXTXCommJar for Arduino, all jars in jetty/lib and jetty/lib/websocket.
	
* How to run:

	There are multiple options for running the code, all displayed at the top of Main.java:
		-SIMULATION: Use if you want to run simulation runs on the computer (no human involved). 
		It will give you three files for the reward, specified in Constants.java (rewardAdaPTName, rewardPRQLName, rewardQLearningName) gained from perturbation teams using AdaPT, perturbation teams using PRQL, and Q-learning from scratch respectively.
		PRQL is run with an empty value function and then is also seeded with value functions from each of the training tasks (so we can evaluate how PRQL performs with good and bad priors).
		We evaluate all of the algorithms given limited simulation time.

		-SIMULATION_HUMAN_TRAIN_TEST: Use if you want to run human subject experiments in which humans work with a simulated robot/agent for BOTH the training and testing phase.
		A GUI will pop up, and then in the console, you will be prompted for the participant's ID and then for the participant's condition (BH - perturbation AdaPT, BQ - perturbation Q-learning, PQ - procedural using Q-learning)
		It will give you a folder with the participant's ID located in the directory indicated by participantDir, as specified in Constants.java.
		This folder contains the reward obtained in each episode in training and testing (RewardHuman), the number of iterations it took to complete each episode (IterHuman), the time it took for each episode (Time), a text file of the states, actions, and rewards from each episode (episode), 
		the number of times the human accepted and rejected the robot's suggestions in each episode (humanAccepts, humanRejects), the number of times the human suggested vs. updated the robot (humanSuggestions, humanUpdates), and finally the equivalent accept, reject, suggestion, update files for the robot.
		
		-SIMULATION_HUMAN_TRAIN: Use if you want to run human subject experiments in which humans work with a simulated robot/agent for ONLY the training phase.
		A GUI will pop up, and then in the console, you will be prompted for the participant's ID and then for the participant's condition (BH - perturbation AdaPT, BQ - perturbation Q-learning, PQ - procedural using Q-learning)
		The same data is given as in SIMULATION_HUMAN_TRAIN_TEST, but only for episodes that were executed (so only training rather than training and testing).
		Also, this data will be saved to a file, which should be in your Dropbox folder, specified by trainedQValuesDir in Constants.java.
		
		-ROBOT_HUMAN_TEST: Use if you want to run human subject experiments in which humans work with an embodied robot (the PR2 in our case) after participants already completed the training phase SIMULATION_HUMAN_TRAIN.
		Make sure the arduino is connected to the computer.
		After running, a server will start, and you will have to connect the two clients (the human through Google Web Speech Recognition and the robot through ROS) before beginning.
		To connect web speech, go to your jetty folder and run java -jar start.jar then go on Chrome to localhost:8080/test. This will connect the human client.
		To connect ROS, first make sure all the needed ROS nodes are up and running, then run python stateMachine_lefty_ramya.py. This will connect the robot client.
		Finally, in the console, you will be prompted for the participant's ID and then for the participant's condition (BH - perturbation AdaPT, BQ - perturbation Q-learning, PQ - procedural using Q-learning).
		This condition will use the Q-values learned in training from SIMULATION_HUMAN_TRAIN so make sure your dropbox folder has the updated training Q-values.
		The same data is given as in SIMULATION_HUMAN_TRAIN_TEST, but only for episodes that were executed (so only testing rather than training and testing).

		CREATE_PREDEFINED: Use if you want to create predefined test cases for human subject experiments (no stochasticity between participants, given a state and joint action, the next state will always be the same across participants).
		Make sure usePredefinedTestCases in Constants.java is false.
		The predefined cases will be written to files specified by predefinedProceFileName, predefinedPerturb1FileName, predefinedPerturb2FileName in Constants.java. 
		Currently, the framework only supports a procedural test case (a practice and then a real procedural test case) and then 2 perturbed test tasks.
		
		CREATE_OFFLINE_QVALUES: Use if you want to run simulations on a deterministic case where the robot learns offline, saves the Q-values to a file, and reads them in before working with a human.
		Change useOfflineValues in Constants.java to be false.
		Change NUM_EPISODES in Constants.java to be the number of episodes the robot should offline learn for (we used 500,000 episodes).
		Change ALPHA to be 1 since we are using a deterministic environment (otherwise we set it here to 0.05).
		The resultant Q-Values after running Q-learning for NUM_EPISODES will be saved to files jointQValuesFile and robotQValuesFile, specified in Constants.java. The joint Q-values are represented by Q(s, a_h, a_r) and the robot Q-values are represented by Q(s, a_r).	
		
		GENERATE_RBM_DATA: Use if you want to get a sample of <state, action, state> pairs from each of the training MDPs and testing MDPs. 
		It will give you a file for each training and testing world (e.g. trainWorld_1, trainWorld_2, testWorld_7, etc).
		Each line specifies one sample from the MDP. It first has the ID of the state, and then the ID of the joint action, and finally the ID of the next state.
		The number of lines are the number of samples/data points from the transition function of that MDP.	
		You can take these tuples of data and input them to an RBM (Restricted Boltzmann Machine) to get a measure of similarity between MDPs (we use this as a comparison to our work).
		More details about this similarity measure using an RBM is in this paper: 
		Ammar, Haitham Bou, et al. "An automated measure of MDP similarity for transfer in reinforcement learning." AAAI Workshop. 2014.
		
		REWARD_OVER_ITERS: Use if you want to compare AdaPT and PRQL over time.
		This option evaluates AdaPT and PRQL at specified intervals until some number of iterations. You can then see how quickly the two algorithms are able to adapt to a new task.
		It will give you one file specified by numIterName in Constants.java. The first part of the file is the reward from AdaPT. After the blank line, you will see the reward from PRQL.
		Each line will have the reward at specified intervals (indicated by INTERVAL in Constants.java) until NUM_EPISODES_TEST.
		So, if INTERVAL = 50 and NUM_EPISODES_TEST = 5000, each line in the file with have a 1000 numbers, the first the reward gained after 50 iterations, than 100 iterations, and so on until 5000.
		The number of lines corresponds to the number of test tasks (if there are 10 test tasks, there will be 10 rows for AdaPT, a blank line, and then 10 rows for PRQL).
		
### Who do I talk to? ###

* If you have any questions/comments, please contact Ramya at ram.ramya@gmail.com.