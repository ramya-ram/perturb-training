N = 3;
trainDataPrefix = 'trainFile';
testDataPrefix = 'testFile';

testDataFile = strcat(testDataPrefix, '1', '.csv');
testdata = csvread(testDataFile);
 
for num=1:N
    disp(strcat('trainingFile ', int2str(num)))
    trainDataFile = strcat(trainDataPrefix, int2str(num), '.csv');
    data = csvread(trainDataFile);

    % Train Gaussian-Bernoulli RBM
    rbm = randRBM(3,5,'GBRBM');
    rbm = pretrainRBM(rbm, data);
    
    %reconstruct the images by going up down then up again using learned model
    up = v2h(rbm, testdata);
    down = h2v(rbm, up);

    errors = zeros(length(testdata), 1);

    for i=1:length(testdata)
        errors(i) = sqrt(sum((testdata(i,:) - down(i,:)) .^ 2));
    end

    meanError = (1/length(testdata))*sum(errors)
end
