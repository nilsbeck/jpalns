package de.nilsbeck.jpalns;

import com.ibm.asyncutil.locks.AsyncLock;

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

import static com.ea.async.Async.await;


public class ParallelAlns<TInput, TSolution extends ISolution<TSolution>> implements ISolve<TInput, TSolution>
{
    private final int _numberOfThreads;
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
    private AsyncLock lock1 = AsyncLock.create();
    private AsyncLock lock2 = AsyncLock.create();
    private AsyncLock lock3 = AsyncLock.create();

    private List<Double> _cumulativeWeights;
    private TSolution _x;

    public ParallelAlns(Function<TInput, TSolution> constructionHeuristic,
                        ArrayList<Function<TSolution, CompletableFuture<TSolution>>> destroyOperators,
                        ArrayList<Function<TSolution, CompletableFuture<TSolution>>> repairOperators, double temperature, double alpha, Random randomizer,
    double newGlobalBestWeight, double betterSolutionWeight, double acceptedSolution, double rejectedSolution,
    double decay, Double initialWeight, Double precision, int numberOfThreads, Function<TSolution, Boolean> abort, Consumer<TSolution> progressUpdate)
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
            double temperature = _temperature;
            do {
                //System.out.println("Apply Application runs on: " + Thread.currentThread().getName());
                Function<TSolution, CompletableFuture<TSolution>> d;
                Function<TSolution, CompletableFuture<TSolution>> r;
                int operatorIndex;
                var locked1 = await(lock1.acquireLock());
                operatorIndex = SelectOperatorIndex(_cumulativeWeights);
                d = _destroyOperators.get(operatorIndex / _repairOperators.size());
                r = _repairOperators.get(operatorIndex % _repairOperators.size());
                locked1.releaseLock();

                TSolution xTemp;
                var locked2 = await(lock2.acquireLock());
                xTemp = _x.Clone();
                locked2.releaseLock();
                xTemp = await(r.apply(await(d.apply(xTemp))));

                WeightSelection weightSelection;
                var locked3 = await(lock2.acquireLock());
                weightSelection = UpdateCurrentSolution(xTemp, temperature);
                locked3.releaseLock();

                var locked4 = await(lock3.acquireLock());
                weightSelection = UpdateBestSolution(xTemp, weightSelection);
                locked4.releaseLock();

                var locked5 = await(lock1.acquireLock());
                UpdateWeights(operatorIndex, weightSelection);
                locked5.releaseLock();

                temperature *= _alpha;

                if (_progressUpdate != null) _progressUpdate.accept(BestSolution);
            } while (!_abort.apply(BestSolution));
        };

        for (int i = 0; i< _numberOfThreads ; i++){
            new Thread(runnable).start();
        }

        return BestSolution;
    }

    private WeightSelection UpdateBestSolution(TSolution xTemp, WeightSelection weightSelection)
    {
        if (BestSolution.getObjective() - xTemp.getObjective() > _precision)
        {
            BestSolution = xTemp;
            weightSelection = WeightSelection.NewGlobalBest;
        }
        return weightSelection;
    }

    private WeightSelection UpdateCurrentSolution(TSolution xTemp, double temperature) {
        WeightSelection weightSelection = Accept(xTemp, temperature);
        if (weightSelection.ordinal() >= WeightSelection.Accepted.ordinal()) {
            _x = xTemp;
        }
        return weightSelection;
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

    private int SelectOperatorIndex(List<Double> cumulativeWeights)
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
        if (_x.getObjective() - newSolution.getObjective() > _precision) {
            return WeightSelection.BetterThanCurrent;
        }
        double probability = Math.exp(-(newSolution.getObjective() - _x.getObjective()) / temperature);
        boolean accepted;
        accepted = _randomizer.nextDouble() <= probability;
        return accepted ? WeightSelection.Accepted : WeightSelection.Rejected;
    }
}
