package de.nilsbeck.knapsack_example;

import de.nilsbeck.jpalns.ISolve;
import de.nilsbeck.jpalns.Sense;
import de.nilsbeck.jpalns.ParallelAlns;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Solves the knapsack problem using the ALNS solver.
 * Implements the ISolve interface.
 * Adds a constructor to initialize the solver with a number of iterations and a
 * seed.
 * Implements Destroy and Repair operators.
 * Implements a createInitialSolution method to create an initial solution.
 * Reports the best solution found and progress every 1 iterations.
 */
public class KnapsackSolver implements ISolve<KnapsackProblem, KnapsackSolution> {
    private final Random random;
    private final int maxIterations;
    private final AtomicInteger iteration = new AtomicInteger(0);
    private static final int REPORT_INTERVAL = 1; // Report every iteration

    public KnapsackSolver(int numIterations, long seed) {
        this.random = new Random(seed);
        this.maxIterations = numIterations;
    }

    @Override
    public KnapsackSolution Solve(KnapsackProblem input) {
        // Create destroy and repair operators
        List<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> destroyOperators = createDestroyOperators();
        List<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> repairOperators = createRepairOperators();

        // Create and configure ALNS solver
        ParallelAlns<KnapsackProblem, KnapsackSolution> alns = new ParallelAlns<>(
                this::createInitialSolution,
                new ArrayList<>(destroyOperators),
                new ArrayList<>(repairOperators),
                2000.0, // temperature - increased to allow more exploration
                0.99,   // alpha - increased to cool down more slowly
                random,
                2.0,    // newGlobalBestWeight - increased to favor operators that find better solutions
                1.0,    // betterSolutionWeight - increased to favor operators that improve solutions
                0.5,    // acceptedSolution - increased to accept more solutions
                0.1,    // rejectedSolution - increased to give rejected operators more chances
                0.99,   // decay - increased to maintain operator weights longer
                1.0,    // initialWeight
                1e-5,   // precision
                4,      // numberOfThreads
                Sense.MAXIMIZE,
                solution -> {
                    // Check if we've reached max iterations before incrementing
                    int current = iteration.get();
                    return current >= maxIterations;
                },
                bestSolution -> {
                    // Only increment if we haven't reached max iterations
                    int current = iteration.get();
                    if (current >= maxIterations) {
                        return;
                    }
                    int currentIteration = iteration.incrementAndGet();
                    // Only report if we haven't exceeded max iterations
                    if (currentIteration <= maxIterations) {
                        if (currentIteration % REPORT_INTERVAL == 0) {
                            System.out.println("Iteration: " + currentIteration + 
                                ", Best value: " + bestSolution.getObjective());
                        }
                        if (currentIteration == maxIterations) {
                            // Print the results
                            System.out.println("--------------------------------");
                            System.out.println("Selected items:");
                            for (int i = 0; i < bestSolution.getProblem().getItems().size(); i++) {
                                if (bestSolution.isItemSelected(i)) {
                                    KnapsackProblem.Item item = bestSolution.getProblem().getItems().get(i);
                                    System.out.printf("Item %d: weight=%d, value=%d%n",
                                            i, item.getWeight(), item.getValue());
                                }
                            }
                            System.out.println("--------------------------------");
                            System.out.println("Best solution found:");
                            System.out.println("Total value: " + bestSolution.getObjective());
                            System.out.println("Total weight: " + bestSolution.getTotalWeight());
                        }
                    }
                });

        // Run the solver
        return alns.Solve(input);
    }

    // Make createInitialSolution public for testing
    public KnapsackSolution createInitialSolution(KnapsackProblem problem) {
        KnapsackSolution solution = new KnapsackSolution(problem);
        List<ItemWithIndex> items = new ArrayList<>();

        // Create list of items with their indices
        for (int i = 0; i < problem.getItems().size(); i++) {
            KnapsackProblem.Item item = problem.getItems().get(i);
            items.add(new ItemWithIndex(i, item));
        }

        // TODO: Uncomment this to use a better initial solution
        // But for this example we turn it off to see the effect from the ALNS and not
        // find the optimal solution in the initial solution.
        // Sort items by value/weight ratio for better initial solution
        // items.sort((a, b) -> Double.compare(
        //     (double) b.item.getValue() / b.item.getWeight(),
        //     (double) a.item.getValue() / a.item.getWeight()
        // ));

        // Add items greedily
        for (ItemWithIndex item : items) {
            if (solution.getTotalWeight() + item.item.getWeight() <= problem.getCapacity()) {
                solution.addItem(item.index);
            }
        }

        System.out.println("Initial solution:");
        System.out.println("Total Value: " + solution.getObjective() + " Total Weight: " + solution.getTotalWeight());
        System.out.println("--------------------------------");
        return solution;
    }

    private List<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> createDestroyOperators() {
        List<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> operators = new ArrayList<>();
        operators.add(solution -> {
            RandomDestroyOperator destroyer = new RandomDestroyOperator(random);
            destroyer.destroy(solution, 0.5); // Increased to 50% to allow more exploration
            return CompletableFuture.completedFuture(solution);
        });
        operators.add(solution -> {
            WorstValueDestroyOperator destroyer = new WorstValueDestroyOperator();
            destroyer.destroy(solution, 0.5); // Increased to 50% to allow more exploration
            return CompletableFuture.completedFuture(solution);
        });
        return operators;
    }

    private List<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> createRepairOperators() {
        List<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> operators = new ArrayList<>();
        operators.add(solution -> {
            GreedyRepairOperator repairer = new GreedyRepairOperator();
            repairer.repair(solution);
            return CompletableFuture.completedFuture(solution);
        });
        operators.add(solution -> {
            RandomRepairOperator repairer = new RandomRepairOperator(random);
            repairer.repair(solution);
            return CompletableFuture.completedFuture(solution);
        });
        return operators;
    }

    private static class ItemWithIndex {
        final int index;
        final KnapsackProblem.Item item;

        ItemWithIndex(int index, KnapsackProblem.Item item) {
            this.index = index;
            this.item = item;
        }
    }

    // Make destroy operators public for testing
    public interface DestroyOperator {
        void destroy(KnapsackSolution solution, double percentage);
    }

    public class RandomDestroyOperator implements DestroyOperator {
        private final Random random;

        RandomDestroyOperator(Random random) {
            this.random = random;
        }

        @Override
        public void destroy(KnapsackSolution solution, double percentage) {
            int numItems = solution.getProblem().getItems().size();
            int numToRemove = (int) (numItems * percentage);

            for (int i = 0; i < numToRemove; i++) {
                int index = random.nextInt(numItems);
                if (solution.isItemSelected(index)) {
                    solution.removeItem(index);
                }
            }
        }
    }

    public class WorstValueDestroyOperator implements DestroyOperator {
        @Override
        public void destroy(KnapsackSolution solution, double percentage) {
            List<ItemWithIndex> selectedItems = new ArrayList<>();
            KnapsackProblem problem = solution.getProblem();

            // Collect selected items
            for (int i = 0; i < problem.getItems().size(); i++) {
                if (solution.isItemSelected(i)) {
                    selectedItems.add(new ItemWithIndex(i, problem.getItems().get(i)));
                }
            }

            // Sort by value/weight ratio
            selectedItems.sort((a, b) -> Double.compare(
                    (double) a.item.getValue() / a.item.getWeight(),
                    (double) b.item.getValue() / b.item.getWeight()));

            // Remove worst items
            int numToRemove = (int) (selectedItems.size() * percentage);
            for (int i = 0; i < numToRemove && i < selectedItems.size(); i++) {
                solution.removeItem(selectedItems.get(i).index);
            }
        }
    }

    // Make repair operators public for testing
    public interface RepairOperator {
        void repair(KnapsackSolution solution);
    }

    public class GreedyRepairOperator implements RepairOperator {
        @Override
        public void repair(KnapsackSolution solution) {
            List<ItemWithIndex> unselectedItems = new ArrayList<>();
            KnapsackProblem problem = solution.getProblem();

            // Collect unselected items
            for (int i = 0; i < problem.getItems().size(); i++) {
                if (!solution.isItemSelected(i)) {
                    unselectedItems.add(new ItemWithIndex(i, problem.getItems().get(i)));
                }
            }

            // Sort by value/weight ratio
            unselectedItems.sort((a, b) -> Double.compare(
                    (double) b.item.getValue() / b.item.getWeight(),
                    (double) a.item.getValue() / a.item.getWeight()));

            // Add items greedily
            for (ItemWithIndex item : unselectedItems) {
                if (solution.getTotalWeight() + item.item.getWeight() <= problem.getCapacity()) {
                    solution.addItem(item.index);
                }
            }
        }
    }

    public class RandomRepairOperator implements RepairOperator {
        private final Random random;

        RandomRepairOperator(Random random) {
            this.random = random;
        }

        @Override
        public void repair(KnapsackSolution solution) {
            List<ItemWithIndex> unselectedItems = new ArrayList<>();
            KnapsackProblem problem = solution.getProblem();

            // Collect unselected items
            for (int i = 0; i < problem.getItems().size(); i++) {
                if (!solution.isItemSelected(i)) {
                    unselectedItems.add(new ItemWithIndex(i, problem.getItems().get(i)));
                }
            }

            // Shuffle items
            for (int i = unselectedItems.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                ItemWithIndex temp = unselectedItems.get(i);
                unselectedItems.set(i, unselectedItems.get(j));
                unselectedItems.set(j, temp);
            }

            // Add items randomly
            for (ItemWithIndex item : unselectedItems) {
                if (solution.getTotalWeight() + item.item.getWeight() <= problem.getCapacity()) {
                    solution.addItem(item.index);
                }
            }
        }
    }
}
