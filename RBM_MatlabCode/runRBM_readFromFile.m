function [closestMDPNum] = runRBM_readFromFile(numTrainingTasks, numDataPoints, numHiddenUnits, testTaskNum)

trainDataPrefix = 'RBM_MatlabCode\\trainworld_fire_';
testDataFile = strcat('RBM_MatlabCode\\testworld_fire_', int2str(testTaskNum), '.csv');

testDataFile = strcat(testDataFile);
testdata = csvread(testDataFile);
testdata = testdata(1:numDataPoints, :);

meanError = zeros(1,numTrainingTasks);
 
for num=1:numTrainingTasks
    trainDataFile = strcat(trainDataPrefix, int2str(num), '.csv');
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

    meanError(1,num) = (1/length(testdata))*sum(errors);
end

[minValue, closestMDPNum] = min(meanError);
meanError
closestMDPNum

end
