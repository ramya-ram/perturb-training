function closestMDPNum = runRBM(traindata_param, testdata_param, numHiddenUnits)

numTrainingTasks = size(traindata_param, 2);
numDataPoints = size(traindata_param{1}, 2);
numFeatures = size(traindata_param{1}{1}, 2);
meanError = zeros(1,numTrainingTasks);

traindata = zeros(numTrainingTasks, numDataPoints, numFeatures);
testdata = zeros(numDataPoints, numFeatures);
for i=1:size(traindata,1)
    for j=1:size(traindata,2)
        traindata(i,j,:) = traindata_param{i}{j};
    end
end

for i=1:size(testdata,1)
    testdata(i,:) = testdata_param{i};
end

for num=1:numTrainingTasks
    data = reshape(traindata(num,:,:), [size(traindata,2),size(traindata,3)]);
    
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

[minValue, minIndices] = min(meanError);
meanError
closestMDPNum = minIndices(1)

end

