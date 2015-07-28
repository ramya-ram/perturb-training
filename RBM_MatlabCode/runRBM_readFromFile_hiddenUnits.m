function [bestNumHiddenUnits] = runRBM_readFromFile_hiddenUnits(numDataPoints)

hiddenUnits = [5, 10, 50, 100, 500, 1000];

trainDataFile = 'trainworld_fire_1.csv';
data = csvread(trainDataFile);
data = data(1:numDataPoints, :);

meanError = zeros(1,size(hiddenUnits,2));

for run=1:size(hiddenUnits,2)

    % Train Gaussian-Bernoulli RBM
    rbm = randRBM(size(data,2), hiddenUnits(1,run), 'GBRBM');
    rbm = pretrainRBM(rbm, data);

    %reconstruct the images by going up down then up again using learned model
    up = v2h(rbm, data);
    down = h2v(rbm, up);

    errors = zeros(length(data), 1);

    for i=1:length(data)
        errors(i) = sqrt(sum((data(i,:) - down(i,:)) .^ 2));
    end

    meanError(1,run) = (1/length(data))*sum(errors);
        
end

meanError
[values, indices] = min(meanError)
bestNumHiddenUnits = hiddenUnits(1,indices(1))

end
