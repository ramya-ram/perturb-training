function [closestMDPNums] = runRBM_readFromFile(numTrainingTasks, numTestTasks, numDataPoints, numHiddenUnits, numRuns)

trainDataPrefix = 'trainworld_gridWorld_';
testDataPrefix = 'testworld_gridWorld_';
allClosestMDPNums = zeros(numRuns,numTestTasks);

for run=1:numRuns
    for testTask=1:numTestTasks

        %testDataFile = strcat(testDataPrefix, int2str(testTask), '.csv');
        testDataFile = strcat(testDataPrefix, '0_4', '.csv');
        testdata = csvread(testDataFile);
        testdata = testdata(1:numDataPoints, :);

        meanError = zeros(1,numTrainingTasks);

        for trainTask=1:numTrainingTasks
            trainDataFile = strcat(trainDataPrefix, int2str(trainTask), '.csv');
            data = csvread(trainDataFile);
            data = data(1:numDataPoints, :);

            % Train Gaussian-Bernoulli RBM
            rbm = randRBM(size(data,2), numHiddenUnits, 'GBRBM');
            rbm = pretrainRBM(rbm, data);

            %reconstruct the images by going up down then up again using learned model
            up = v2h(rbm, testdata);
            down = h2v(rbm, up);

            errors = zeros(length(testdata), 1);

            for i=1:length(testdata)
                errors(i) = sqrt(sum((testdata(i,:) - down(i,:)) .^ 2));
            end

            meanError(1,trainTask) = (1/length(testdata))*sum(errors);
        end

        [minValue, closestMDPNum] = min(meanError);
        allClosestMDPNums(run,testTask) = closestMDPNum-1;
        %meanError
        %closestMDPNum

    end
    
end

allClosestMDPNums
closestMDPNums = mode(allClosestMDPNums,1)

end
