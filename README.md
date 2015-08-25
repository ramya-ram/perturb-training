# README #

This README explains how to use this code to run computational simulations and human subject experiments.

### What is this repository for? ###

* This repository holds the code for human-robot perturbation training.
* It can be used to run human subject experiments with a simulated robot and with an embodied robot (the PR2).
* It can also be used to run computational simulations without a human over two domains.

### How do I get set up? ###

* Summary of set up:
	Make sure you download the Java code base, which should have source files as well as input files (predefined cases, offline learning values) and data (pictures used for the GUI).
	
	For running robot experiments, you will need an Ubuntu machine with ROS installed (I use VMWare Workstation to have an Ubuntu virtual box on my Windows machine)
	On my virtual machine, I have the humanExperiments/ROS/local unzipped folder located at /home/local in Ubuntu
	I have the humanExperiments/ROS/sandbox unzipped folder located at /home/fuerte_workspace/sandbox (this sandbox folder is what the ROS tutorials had me create when I went through them)
	
	You will also need the Arduino environment installed to control the lights using the code (represents different intensities of fires)
	You can connect your laptop to an Arduino using a serial cable and upload humanExperiments/Arduino/LightUpFires.ino to the Arduino so it has the code to accordingly light up the LEDs
	
	Finally you'll need Google Web Speech Recognition. Install Jetty. On my Windows machine, I have the humanExperiments/GoogleWebSpeech/test unzipped folder located at jetty/webapps/test/.

	You will need to add to add some external jars to your project in Eclipse: all jars in lib/.
	
* How to run:

	There are multiple options for running the code, all displayed at the top of Main.java. Set Main.INPUT to be one of these options.
	
		-SIMULATION_HUMAN_TRAIN_TEST: Use if you want to run human subject experiments in which humans work with a simulated robot/agent for BOTH the training and testing phase.
		A GUI will pop up, and then in the console, you will be prompted for the participant's ID and then for the participant's condition (BH - perturbation AdaPT, BQ - perturbation Q-learning, PQ - procedural using Q-learning)
		It will give you a folder with the participant's ID located in the directory indicated by participantDir, as specified in Constants.java.
		This folder contains the reward obtained in each episode in training and testing (RewardHuman), the number of iterations it took to complete each episode (IterHuman), the time it took for each episode (Time), a text file of the states, actions, and rewards from each episode (episode), 
		the number of times the human accepted and rejected the robot's suggestions in each episode (humanAccepts, humanRejects), the number of times the human suggested vs. updated the robot (humanSuggestions, humanUpdates), and finally the equivalent accept, reject, suggestion, update files for the robot.
		
		-SIMULATION_HUMAN_TRAIN: Use if you want to run human subject experiments in which humans work with a simulated robot/agent for ONLY the training phase.
		A GUI will pop up, and then in the console, you will be prompted for the participant's ID and then for the participant's condition (BH - perturbation AdaPT, BQ - perturbation Q-learning, PQ - procedural using Q-learning)
		The same data is given as in SIMULATION_HUMAN_TRAIN_TEST, but only for episodes that were executed (so only training rather than training and testing).
		Also, this data will be saved to a file, specified by trainedQValuesDir in Constants.java.
		
		-ROBOT_HUMAN_TEST: Use if you want to run human subject experiments in which humans work with an embodied robot (the PR2 in our case) after participants already completed the training phase SIMULATION_HUMAN_TRAIN.
		Make sure the arduino is connected to the computer.
		After running, a server will start, and you will have to connect the two clients (the human through Google Web Speech Recognition and the robot through ROS) before beginning.
		To connect web speech, go to your jetty folder and run java -jar start.jar then go on Chrome to localhost:8080/test. This will connect the human client.
		To connect ROS, first make sure all the needed ROS nodes are up and running, then run python stateMachine_lefty_fireTask.py. This will connect the robot client.
		Finally, in the console, you will be prompted for the participant's ID and then for the participant's condition (BH - perturbation AdaPT, BQ - perturbation Q-learning, PQ - procedural using Q-learning).
		This condition will use the Q-values learned in training from SIMULATION_HUMAN_TRAIN so make sure you have the updated training Q-values in the directory specified by trainedQValuesDir in Constants.java.
		The same data is given as in SIMULATION_HUMAN_TRAIN_TEST, but only for episodes that were executed (so only testing rather than training and testing).

		CREATE_PREDEFINED: Use if you want to create predefined test cases for human subject experiments (no stochasticity between participants, given a state and joint action, the next state will always be the same across participants).
		Make sure usePredefinedTestCases in Constants.java is false.
		The predefined cases will be written to files specified by predefinedProceFileName, predefinedPerturb1FileName, predefinedPerturb2FileName in Constants.java. 
		Currently, the framework creates 1 procedural test case (used for both a practice execution and a real execution used in the evaluation) and then 2 perturbed test cases for the fire task domain.
		
		CREATE_OFFLINE_QVALUES: Use if you want to run simulations on a deterministic case where the robot learns offline, saves the Q-values to a file, and reads them in before working with a human.
		Change useOfflineValues in Constants.java to be false.
		Change NUM_EPISODES in Constants.java to be the number of episodes the robot should offline learn for (we used 500,000 episodes for the fire task domain).
		Change ALPHA to be 1 since we are using a deterministic environment (otherwise we set it here to 0.05).
		The resultant Q-Values after running Q-learning for NUM_EPISODES will be saved to files jointQValuesFile and robotQValuesFile, specified in Constants.java. The joint Q-values are represented by Q(s, a_h, a_r) and the robot Q-values are represented by Q(s, a_r).	
		
		REWARD_OVER_ITERS: Use if you want to compare algorithms over time.
		This option evaluates AdaPT, PRQL, PRQL-RBM, and Q-learning at specified intervals until some number of iterations. You can then see how quickly the algorithms are able to adapt to a new task.
		It will give you one file specified by rewardOverIters in Constants.java. This will write, for each test case, a row of numbers that can be plotted to show reward over time. Between each condition (e.g. AdaPT, PRQL), there will be a new line.
		Each line will have the reward at specified intervals (indicated by INTERVAL in Constants.java) until NUM_EPISODES_TEST.
		So, if INTERVAL = 50 and NUM_EPISODES_TEST = 5000, each line in the file with have a 1000 numbers, the first the reward gained after 50 iterations, than 100 iterations, and so on until 5000.
		The number of lines corresponds to the number of test tasks (if there are 10 test tasks, there will be 10 rows for AdaPT, a blank line, and then 10 rows for PRQL, etc).
		It will also give a separate file for each algorithm (specified by rewardOverItersData in Constants.java followed by the algorithm name) that records all of the simulation runs (not just the average). In these files, the rows represent the different simulation runs and there is a space in each row separating different test cases.
		The separate files are mainly used if something happens in the middle of your program execution that could cause you to lose all your data. If the whole program finishes, you can simply use the average file, specified by rewardOverIters in Constants.java.
		
		-REWARD_LIMITED_TIME: Use if you want to compare algorithms given limited simulation time.
		It will give you a file specified by rewardLimitedTime in Constants.java which gives the average reward over all simulation runs for different test cases.
		Each algorithm will have one row. The number of columns will be the number of test cases, so the value in each column specifies the reward obtained from that test task after simulating for a limited number of iterations (specified by NUM_EPISODES_TEST in Constants.java).
		It will also give you another file with all the data (not just the averages) specified by rewardLimitedTimeData in Constants.java.
		This will print out a simulation run in each row. The columns have different test cases and conditions (e.g. AdaPT, PRQL) are separated by a space.
		PRQL is run with an empty value function and then is also seeded with value functions from each of the training tasks (so we can evaluate how PRQL performs with good and bad priors).
		We evaluate all of the algorithms given limited simulation time.
		
### Who do I talk to? ###

* If you have any questions/comments, please contact Ramya at ram.ramya@gmail.com.