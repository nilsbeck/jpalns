package de.nilsbeck.jpalns;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelAlns<TInput, TSolution extends ISolution<TSolution>> implements ISolve<TInput, TSolution>
{
    private final int _numberOfThreads;
    private final Sense _optimizationType;
    private Function<TSolution, Boolean> _abort;
    private double _alpha; //>0 and < 1
    private Function<TInput, TSolution> _constructionHeuristic;

    private List<Function<TSolution, CompletableFuture<TSolution>>> _destroyOperators;
    private List<Double> _weights;

    private double _newGlobalBestWeight,
        _betterSolutionWeight,
        _acceptedSolution,
        _rejectedSolution,
        _decay,
        _precision,
        _initialWeight;

    /// <summary>
    /// Called after every iteration with the current best solution as input.
    /// </summary>
    private Consumer<TSolution> _progressUpdate;

    private Random _randomizer;
    private List<Function<TSolution, CompletableFuture<TSolution>>> _repairOperators;
    private double _temperature; // >0
    private final ReentrantLock lock1 = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();
    private final ReentrantLock lock3 = new ReentrantLock();

    private List<Double> _cumulativeWeights;
    private TSolution _x;

    public ParallelAlns(Function<TInput, TSolution> constructionHeuristic,
                        ArrayList<Function<TSolution, CompletableFuture<TSolution>>> destroyOperators,
                        ArrayList<Function<TSolution, CompletableFuture<TSolution>>> repairOperators, double temperature, double alpha, Random randomizer,
    double newGlobalBestWeight, double betterSolutionWeight, double acceptedSolution, double rejectedSolution,
    double decay, Double initialWeight, Double precision, int numberOfThreads, Sense optimizationType, Function<TSolution, Boolean> abort, Consumer<TSolution> progressUpdate)
    {
        _destroyOperators = destroyOperators;
        _repairOperators = repairOperators;
        _temperature = temperature;
        _alpha = alpha;
        _randomizer = randomizer;
        _newGlobalBestWeight = newGlobalBestWeight;
        _betterSolutionWeight = betterSolutionWeight;
        _acceptedSolution = acceptedSolution;
        _rejectedSolution = rejectedSolution;
        _decay = decay;
        _initialWeight = initialWeight != null ? initialWeight : 1;
        _precision = precision != null ? precision : 1e-5;
        _abort = abort;
        _numberOfThreads = numberOfThreads;
        _optimizationType = optimizationType;
        _progressUpdate = progressUpdate;
        _constructionHeuristic = constructionHeuristic;
        _weights = _destroyOperators.stream().flatMap(destroy -> _repairOperators.stream().map(repair -> _initialWeight)).collect(Collectors.toCollection(ArrayList::new));

        _cumulativeWeights = Helper.toCumulativeEnumerable(_weights);
    }

    /// <summary>
    /// Holds the current best solution
    /// </summary>
    private TSolution BestSolution;

    /// <summary>
    /// Gets a text describing the methods' weights.
    /// </summary>
    public String getMethodWeightLog() {
        return WeightLog(
                "Operators' weights",
                _weights.stream().mapToDouble(d -> d).toArray(),
                idx -> MessageFormat.format("{0}, {1}",
                    _destroyOperators.get(idx / _repairOperators.size()).toString(), _repairOperators.get(idx % _repairOperators.size()).getClass().getName()));
    }

    /// <summary>
    /// Gets a text describing the repair operations' weight:
    /// The weight of one repair operation is the average of the weights of all operations where the repair is used.
    /// </summary>
    public String getRepairWeightLog() {
        /* Create an array of doubles describing the weight of each repair operation */
        double[] repairWeights = new double[_repairOperators.size()];
        for (int repairIdx = 0; repairIdx < repairWeights.length; repairIdx++)
        {
            int finalRepairIdx = repairIdx;
            repairWeights[repairIdx] =
                    IntStream.range(0, _destroyOperators.size()).map(i -> i * _repairOperators.size()).mapToDouble(destroyIdx -> _weights.get(destroyIdx + finalRepairIdx)).average().getAsDouble();
        }

        /* Create and return description */
        return WeightLog("Total repair weights", repairWeights, idx -> _repairOperators.get(idx).getClass().getName());
    }


    /// <summary>
    /// Gets a text describing the destroy operations' weight:
    /// The weight of one destroy operation is the average of the weights of all operations where the destroy is used.
    /// </summary>
    public String getDestroyWeightLog() {
        /* Create an array of doubles describing the weight of each destroy operation */
        double[] destroyWeights = new double[_destroyOperators.size()];
        for (int destroyIdx = 0; destroyIdx < destroyWeights.length; destroyIdx++)
        {
            int finalDestroyIdx = destroyIdx;
            double destroyWeight = IntStream.range(0, _repairOperators.size()).mapToDouble(repairIdx -> _weights.get(finalDestroyIdx * _repairOperators.size() + repairIdx)).average().getAsDouble();
            destroyWeights[destroyIdx] = destroyWeight;
        }

            /* Create and return description */
        return WeightLog("Total destroy weights", destroyWeights, idx -> _destroyOperators.get(idx).getClass().getName());
    }

    /// <summary>
    /// Returns a nicely formatted overview over weights for operations, including both total and relative weights.
    /// </summary>
    /// <param name="title">The overview's title.</param>
    /// <param name="weights">Weights to use.</param>
    /// <param name="operationNameFromIdx">Given a (weight) index, returns the correct operation.</param>
    /// <returns>A string describing the distribution of weight between operations.</returns>
    private String WeightLog(String title, double[] weights, Function<Integer, String> operationNameFromIdx)
    {
        StringBuilder log = new StringBuilder(String.format("%s\nWeight Probability Operation\n\n", title));
        double sum = Arrays.stream(weights).sum();
        for (int idx : IntStream.range(0, weights.length).toArray()) {
            log.append(weights[idx]).append("   ").append(weights[idx] / sum).append("   ").append(operationNameFromIdx.apply(idx));
        }

        return log.toString();
    }

    /// <summary>
    /// Solves the given input and produces a solution
    /// </summary>
    /// <param name="input">The problem to solve</param>
    /// <returns>A solution to the input</returns>
    public TSolution Solve(TInput input) {
        _x = _constructionHeuristic.apply(input);
        BestSolution = _x;

        Runnable runnable = () -> {
            final double[] temperature = {_temperature};
            do {
                @SuppressWarnings("unchecked")
                final CompletableFuture<Function<TSolution, CompletableFuture<TSolution>>>[] dFuture = 
                    (CompletableFuture<Function<TSolution, CompletableFuture<TSolution>>>[]) new CompletableFuture<?>[1];
                @SuppressWarnings("unchecked")
                final CompletableFuture<Function<TSolution, CompletableFuture<TSolution>>>[] rFuture = 
                    (CompletableFuture<Function<TSolution, CompletableFuture<TSolution>>>[]) new CompletableFuture<?>[1];
                final int[] operatorIndex = new int[1];
                
                // Select operator
                CompletableFuture<Void> selectOp = new CompletableFuture<>();
                lock1.lock();
                try {
                    operatorIndex[0] = SelectOperatorIndex(_cumulativeWeights);
                    dFuture[0] = CompletableFuture.completedFuture(_destroyOperators.get(operatorIndex[0] / _repairOperators.size()));
                    rFuture[0] = CompletableFuture.completedFuture(_repairOperators.get(operatorIndex[0] % _repairOperators.size()));
                    selectOp.complete(null);
                } finally {
                    lock1.unlock();
                }

                // Clone current solution
                CompletableFuture<TSolution> xTempFuture = new CompletableFuture<>();
                lock2.lock();
                try {
                    xTempFuture.complete(_x.Clone());
                } finally {
                    lock2.unlock();
                }

                // Apply destroy and repair
                CompletableFuture<TSolution> newSolutionFuture = CompletableFuture.allOf(selectOp, xTempFuture)
                    .thenApply(v -> xTempFuture.join())
                    .thenCompose(xTemp -> dFuture[0].thenCompose(d -> d.apply(xTemp)))
                    .thenCompose(xTemp -> rFuture[0].thenCompose(r -> r.apply(xTemp)));

                // Update current solution
                CompletableFuture<WeightSelection> weightSelectionFuture = new CompletableFuture<>();
                lock2.lock();
                try {
                    TSolution xTemp = newSolutionFuture.join();
                    WeightSelection ws = UpdateCurrentSolution(xTemp, temperature[0]);
                    if (ws.ordinal() >= WeightSelection.Accepted.ordinal()) {
                        _x = xTemp;
                    }
                    weightSelectionFuture.complete(ws);
                } finally {
                    lock2.unlock();
                }

                // Update best solution
                CompletableFuture<WeightSelection> finalWeightSelectionFuture = new CompletableFuture<>();
                lock3.lock();
                try {
                    TSolution xTemp = newSolutionFuture.join();
                    WeightSelection ws = weightSelectionFuture.join();
                    finalWeightSelectionFuture.complete(UpdateBestSolution(xTemp, ws));
                } finally {
                    lock3.unlock();
                }

                // Update weights
                CompletableFuture<Void> updateWeightsFuture = new CompletableFuture<>();
                lock1.lock();
                try {
                    WeightSelection ws = finalWeightSelectionFuture.join();
                    UpdateWeights(operatorIndex[0], ws);
                    updateWeightsFuture.complete(null);
                } finally {
                    lock1.unlock();
                }

                // Wait for all operations to complete
                CompletableFuture.allOf(updateWeightsFuture).join();

                temperature[0] *= _alpha;

                if (_progressUpdate != null) _progressUpdate.accept(BestSolution);
            } while (!_abort.apply(BestSolution));
        };

        for (int i = 0; i < _numberOfThreads; i++) {
            new Thread(runnable).start();
        }

        return BestSolution;
    }

    WeightSelection UpdateBestSolution(TSolution xTemp, WeightSelection weightSelection)
    {
        if (_optimizationType.isBetter(xTemp.getObjective(), BestSolution.getObjective(), _precision))
        {
            BestSolution = xTemp;
            weightSelection = WeightSelection.NewGlobalBest;
        }
        return weightSelection;
    }

    WeightSelection UpdateCurrentSolution(TSolution xTemp, double temperature) {
        WeightSelection weightSelection = Accept(xTemp, temperature);
        if (weightSelection.ordinal() >= WeightSelection.Accepted.ordinal()) {
            _x = xTemp;
        }
        return weightSelection;
    }

    int SelectOperatorIndex(List<Double> cumulativeWeights)
    {
        double randomValue;
        randomValue = _randomizer.nextDouble();

        for (int i = 0; i < cumulativeWeights.size(); i++) {
            if (cumulativeWeights.get(i) > randomValue)
                return i;
        }
        return cumulativeWeights.size() - 1;
    }

    private WeightSelection Accept(TSolution newSolution, double temperature)
    {
        if (_optimizationType.isBetter(newSolution.getObjective(), _x.getObjective(), _precision)) {
            return WeightSelection.BetterThanCurrent;
        }
        double probability = _optimizationType.getAcceptanceProbability(newSolution.getObjective(), _x.getObjective(), temperature);
        double randomValue = _randomizer.nextDouble();
        return randomValue <= probability ? WeightSelection.Accepted : WeightSelection.Rejected;
    }

    private void UpdateWeights(int operatorIndex, WeightSelection weightSelection)
    {
        double weight;
        switch (weightSelection)
        {
            case Rejected:
                weight = _rejectedSolution;
                break;
            case Accepted:
                weight = _acceptedSolution;
                break;
            case BetterThanCurrent:
                weight = _betterSolutionWeight;
                break;
            case NewGlobalBest:
                weight = _newGlobalBestWeight;
                break;
            default:
                throw new IllegalArgumentException("WeightSelection has unexpected status");
        }
        _weights.set(operatorIndex,_decay * _weights.get(operatorIndex) + (1 - _decay) * weight);
        _cumulativeWeights = Helper.toCumulativeEnumerable(_weights);
    }
}
