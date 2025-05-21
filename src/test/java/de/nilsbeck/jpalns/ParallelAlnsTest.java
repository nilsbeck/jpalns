package de.nilsbeck.jpalns;

import de.nilsbeck.jpalns.knapsack_example.KnapsackProblem;
import de.nilsbeck.jpalns.knapsack_example.KnapsackSolution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

class ParallelAlnsTest {
    private ParallelAlns<KnapsackProblem, KnapsackSolution> alns;
    private List<Double> initialWeights;
    private static final double DECAY = 0.95;
    private static final double NEW_GLOBAL_BEST_WEIGHT = 1.0;
    private static final double BETTER_SOLUTION_WEIGHT = 0.5;
    private static final double ACCEPTED_SOLUTION_WEIGHT = 0.1;
    private static final double REJECTED_SOLUTION_WEIGHT = 0.0;
    private static final double INITIAL_WEIGHT = 1.0;
    private static final double PRECISION = 1e-5;

    @BeforeEach
    void setUp() {
        // Create multiple destroy and repair operators for testing
        ArrayList<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> destroyOperators = new ArrayList<>();
        ArrayList<Function<KnapsackSolution, CompletableFuture<KnapsackSolution>>> repairOperators = new ArrayList<>();
        
        // Add two destroy operators
        destroyOperators.add(solution -> CompletableFuture.completedFuture(solution));
        destroyOperators.add(solution -> CompletableFuture.completedFuture(solution));
        
        // Add two repair operators
        repairOperators.add(solution -> CompletableFuture.completedFuture(solution));
        repairOperators.add(solution -> CompletableFuture.completedFuture(solution));

        // Create ALNS instance with test parameters
        alns = new ParallelAlns<>(
            input -> new KnapsackSolution(input),
            destroyOperators,
            repairOperators,
            1000.0, // temperature
            0.99,   // alpha
            new Random(42),
            NEW_GLOBAL_BEST_WEIGHT,
            BETTER_SOLUTION_WEIGHT,
            ACCEPTED_SOLUTION_WEIGHT,
            REJECTED_SOLUTION_WEIGHT,
            DECAY,
            INITIAL_WEIGHT,
            PRECISION,
            1,      // numberOfThreads
            Sense.MAXIMIZE,
            solution -> false, // abort function
            solution -> {}     // progress update
        );

        // Get initial weights through reflection
        try {
            java.lang.reflect.Field weightsField = ParallelAlns.class.getDeclaredField("_weights");
            weightsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Double> weights = (List<Double>) weightsField.get(alns);
            initialWeights = new ArrayList<>(weights);
        } catch (Exception e) {
            fail("Failed to access weights field: " + e.getMessage());
        }
    }

    @ParameterizedTest
    @EnumSource(WeightSelection.class)
    void testUpdateWeightsForAllSelections(WeightSelection selection) {
        // Get initial weights
        List<Double> initialWeightsCopy = new ArrayList<>(initialWeights);
        
        // Update weight for operator 0
        try {
            java.lang.reflect.Method updateWeightsMethod = ParallelAlns.class.getDeclaredMethod("UpdateWeights", int.class, WeightSelection.class);
            updateWeightsMethod.setAccessible(true);
            updateWeightsMethod.invoke(alns, 0, selection);
        } catch (Exception e) {
            fail("Failed to call UpdateWeights: " + e.getMessage());
        }

        // Get updated weights
        List<Double> updatedWeights = null;
        try {
            java.lang.reflect.Field weightsField = ParallelAlns.class.getDeclaredField("_weights");
            weightsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Double> weights = (List<Double>) weightsField.get(alns);
            updatedWeights = new ArrayList<>(weights);
        } catch (Exception e) {
            fail("Failed to access weights field: " + e.getMessage());
        }
        
        // Calculate expected weight based on selection
        double expectedWeight;
        switch (selection) {
            case NewGlobalBest:
                expectedWeight = DECAY * initialWeightsCopy.get(0) + (1 - DECAY) * NEW_GLOBAL_BEST_WEIGHT;
                break;
            case BetterThanCurrent:
                expectedWeight = DECAY * initialWeightsCopy.get(0) + (1 - DECAY) * BETTER_SOLUTION_WEIGHT;
                break;
            case Accepted:
                expectedWeight = DECAY * initialWeightsCopy.get(0) + (1 - DECAY) * ACCEPTED_SOLUTION_WEIGHT;
                break;
            case Rejected:
                expectedWeight = DECAY * initialWeightsCopy.get(0) + (1 - DECAY) * REJECTED_SOLUTION_WEIGHT;
                break;
            default:
                fail("Unexpected weight selection: " + selection);
                return;
        }

        // Verify the updated weight
        assertEquals(expectedWeight, updatedWeights.get(0), PRECISION, 
            "Weight update incorrect for " + selection);
        
        // Verify other weights remain unchanged
        for (int i = 1; i < updatedWeights.size(); i++) {
            assertEquals(initialWeightsCopy.get(i), updatedWeights.get(i), PRECISION,
                "Weight at index " + i + " should remain unchanged");
        }
    }

    @Test
    void testCumulativeWeightsUpdate() {
        // Get initial cumulative weights
        List<Double> initialCumulativeWeights = null;
        try {
            java.lang.reflect.Field cumulativeWeightsField = ParallelAlns.class.getDeclaredField("_cumulativeWeights");
            cumulativeWeightsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Double> cumulativeWeights = (List<Double>) cumulativeWeightsField.get(alns);
            initialCumulativeWeights = new ArrayList<>(cumulativeWeights);
        } catch (Exception e) {
            fail("Failed to access cumulative weights field: " + e.getMessage());
        }

        // Update weight for operator 0
        try {
            java.lang.reflect.Method updateWeightsMethod = ParallelAlns.class.getDeclaredMethod("UpdateWeights", int.class, WeightSelection.class);
            updateWeightsMethod.setAccessible(true);
            updateWeightsMethod.invoke(alns, 0, WeightSelection.Rejected);
        } catch (Exception e) {
            fail("Failed to call UpdateWeights: " + e.getMessage());
        }

        // Get updated cumulative weights
        List<Double> updatedCumulativeWeights = null;
        try {
            java.lang.reflect.Field cumulativeWeightsField = ParallelAlns.class.getDeclaredField("_cumulativeWeights");
            cumulativeWeightsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Double> cumulativeWeights = (List<Double>) cumulativeWeightsField.get(alns);
            updatedCumulativeWeights = new ArrayList<>(cumulativeWeights);
        } catch (Exception e) {
            fail("Failed to access cumulative weights field: " + e.getMessage());
        }

        // Verify cumulative weights are updated and normalized
        assertNotNull(initialCumulativeWeights, "Initial cumulative weights should not be null");
        assertNotNull(updatedCumulativeWeights, "Updated cumulative weights should not be null");
        assertFalse(updatedCumulativeWeights.isEmpty(), "Cumulative weights should not be empty");
        assertEquals(1.0, updatedCumulativeWeights.get(updatedCumulativeWeights.size() - 1), PRECISION,
            "Last cumulative weight should be 1.0");
        
        // Verify that the cumulative weights are different from initial state
        // and that they are properly normalized (sum to 1.0)
        double sum = 0.0;
        for (int i = 0; i < updatedCumulativeWeights.size(); i++) {
            if (i > 0) {
                assertTrue(updatedCumulativeWeights.get(i) > updatedCumulativeWeights.get(i-1),
                    "Cumulative weights should be monotonically increasing");
            }
            sum += updatedCumulativeWeights.get(i) - (i > 0 ? updatedCumulativeWeights.get(i-1) : 0.0);
        }
        assertEquals(1.0, sum, PRECISION, "Normalized weights should sum to 1.0");
        
        // Verify that the weights have actually changed
        assertNotEquals(initialCumulativeWeights, updatedCumulativeWeights,
            "Cumulative weights should be updated after weight change");
    }

    @Test
    void testMultipleWeightUpdates() {
        // Perform multiple weight updates
        try {
            java.lang.reflect.Method updateWeightsMethod = ParallelAlns.class.getDeclaredMethod("UpdateWeights", int.class, WeightSelection.class);
            updateWeightsMethod.setAccessible(true);

            // First update with NewGlobalBest
            updateWeightsMethod.invoke(alns, 0, WeightSelection.NewGlobalBest);
            List<Double> weightsAfterFirstUpdate;
            try {
                java.lang.reflect.Field weightsField = ParallelAlns.class.getDeclaredField("_weights");
                weightsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Double> weights = (List<Double>) weightsField.get(alns);
                weightsAfterFirstUpdate = new ArrayList<>(weights);
            } catch (Exception e) {
                fail("Failed to access weights field: " + e.getMessage());
                return;
            }

            // Second update with Rejected
            updateWeightsMethod.invoke(alns, 0, WeightSelection.Rejected);
            List<Double> weightsAfterSecondUpdate;
            try {
                java.lang.reflect.Field weightsField = ParallelAlns.class.getDeclaredField("_weights");
                weightsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Double> weights = (List<Double>) weightsField.get(alns);
                weightsAfterSecondUpdate = new ArrayList<>(weights);
            } catch (Exception e) {
                fail("Failed to access weights field: " + e.getMessage());
                return;
            }

            // Verify weights are properly decayed
            assertTrue(weightsAfterSecondUpdate.get(0) < weightsAfterFirstUpdate.get(0),
                "Weight should decrease after Rejected update");
            assertTrue(weightsAfterSecondUpdate.get(0) > 0,
                "Weight should remain positive after multiple updates");
                
            // Verify other weights remain unchanged
            for (int i = 1; i < weightsAfterSecondUpdate.size(); i++) {
                assertEquals(weightsAfterFirstUpdate.get(i), weightsAfterSecondUpdate.get(i), PRECISION,
                    "Weight at index " + i + " should remain unchanged");
            }
        } catch (Exception e) {
            fail("Failed to perform multiple weight updates: " + e.getMessage());
        }
    }

    @Test
    void testWeightBounds() {
        try {
            java.lang.reflect.Method updateWeightsMethod = ParallelAlns.class.getDeclaredMethod("UpdateWeights", int.class, WeightSelection.class);
            updateWeightsMethod.setAccessible(true);

            // Perform multiple updates to test weight bounds
            for (int i = 0; i < 100; i++) {
                updateWeightsMethod.invoke(alns, 0, WeightSelection.NewGlobalBest);
                List<Double> currentWeights;
                try {
                    java.lang.reflect.Field weightsField = ParallelAlns.class.getDeclaredField("_weights");
                    weightsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<Double> weights = (List<Double>) weightsField.get(alns);
                    currentWeights = new ArrayList<>(weights);
                } catch (Exception e) {
                    fail("Failed to access weights field: " + e.getMessage());
                    return;
                }
                
                // Verify weight bounds for all weights
                for (int j = 0; j < currentWeights.size(); j++) {
                    assertTrue(currentWeights.get(j) >= 0, 
                        "Weight at index " + j + " should never be negative");
                    assertTrue(currentWeights.get(j) <= NEW_GLOBAL_BEST_WEIGHT,
                        "Weight at index " + j + " should not exceed the maximum weight value");
                }
            }
        } catch (Exception e) {
            fail("Failed to test weight bounds: " + e.getMessage());
        }
    }

    @Test
    void testUpdateCurrentSolution() {
        // Create test problem with items
        List<KnapsackProblem.Item> items = Arrays.asList(
            new KnapsackProblem.Item("item1", 10, 100),  // weight 10, value 100
            new KnapsackProblem.Item("item2", 20, 200)   // weight 20, value 200
        );
        KnapsackProblem problem = new KnapsackProblem(items, 100); // capacity 100

        // Create test solutions
        KnapsackSolution currentSolution = new KnapsackSolution(problem);
        KnapsackSolution betterSolution = new KnapsackSolution(problem);
        KnapsackSolution worseSolution = new KnapsackSolution(problem);

        // Add items to create different objective values
        currentSolution.addItem(0); // Add first item (value 100)
        betterSolution.addItem(0);
        betterSolution.addItem(1); // Add both items (value 300)
        worseSolution.addItem(0);
        worseSolution.removeItem(0); // Empty solution (value 0)

        // Test better solution (should be accepted immediately)
        try {
            java.lang.reflect.Field xField = ParallelAlns.class.getDeclaredField("_x");
            xField.setAccessible(true);
            xField.set(alns, currentSolution);  // Reset _x before test
        } catch (Exception e) {
            fail("Failed to reset _x field: " + e.getMessage());
        }
        WeightSelection result = alns.UpdateCurrentSolution(betterSolution, 1000.0);
        assertEquals(WeightSelection.BetterThanCurrent, result, "Better solution should be accepted with BetterThanCurrent status");

        // Test worse solution with high temperature (should have high acceptance probability)
        try {
            java.lang.reflect.Field xField = ParallelAlns.class.getDeclaredField("_x");
            xField.setAccessible(true);
            xField.set(alns, currentSolution);  // Reset _x before test
        } catch (Exception e) {
            fail("Failed to reset _x field: " + e.getMessage());
        }
        result = alns.UpdateCurrentSolution(worseSolution, 1000.0);
        assertTrue(result == WeightSelection.Accepted || result == WeightSelection.Rejected,
            "Worse solution should be either Accepted or Rejected based on probability");

        // Test worse solution with low temperature (should have low acceptance probability)
        try {
            java.lang.reflect.Field xField = ParallelAlns.class.getDeclaredField("_x");
            xField.setAccessible(true);
            xField.set(alns, currentSolution);  // Reset _x before test
        } catch (Exception e) {
            fail("Failed to reset _x field: " + e.getMessage());
        }
        // Set randomizer to always return a value that's guaranteed to be greater than the acceptance probability
        try {
            java.lang.reflect.Field randomizerField = ParallelAlns.class.getDeclaredField("_randomizer");
            randomizerField.setAccessible(true);
            randomizerField.set(alns, new Random() {
                @Override
                public double nextDouble() {
                    return 0.5; // Use a value that's definitely greater than exp(-1000) but less than 1.0
                }
            });
        } catch (Exception e) {
            fail("Failed to set _randomizer field: " + e.getMessage());
        }
        // Test worse solution with low temperature
        double temp = 0.1;
        result = alns.UpdateCurrentSolution(worseSolution, temp);
        assertEquals(WeightSelection.Rejected, result, "Worse solution should be rejected with low temperature");
    }

    @Test
    void testUpdateBestSolution() {
        // Create test problem with items
        List<KnapsackProblem.Item> items = Arrays.asList(
            new KnapsackProblem.Item("item1", 10, 100),  // weight 10, value 100
            new KnapsackProblem.Item("item2", 20, 200)   // weight 20, value 200
        );
        KnapsackProblem problem = new KnapsackProblem(items, 100); // capacity 100

        // Create test solutions
        KnapsackSolution initialBest = new KnapsackSolution(problem);
        KnapsackSolution betterSolution = new KnapsackSolution(problem);
        KnapsackSolution worseSolution = new KnapsackSolution(problem);

        // Set up initial best solution
        initialBest.addItem(0); // Add first item (value 100)
        betterSolution.addItem(0);
        betterSolution.addItem(1); // Add both items (value 300)
        worseSolution.addItem(0);
        worseSolution.removeItem(0); // Empty solution (value 0)

        // Set initial best solution and current solution through reflection
        try {
            java.lang.reflect.Field bestSolutionField = ParallelAlns.class.getDeclaredField("BestSolution");
            bestSolutionField.setAccessible(true);
            bestSolutionField.set(alns, initialBest);
            java.lang.reflect.Field xField = ParallelAlns.class.getDeclaredField("_x");
            xField.setAccessible(true);
            xField.set(alns, initialBest);
        } catch (Exception e) {
            fail("Failed to set BestSolution or _x field: " + e.getMessage());
        }

        // Test with better solution
        WeightSelection result = alns.UpdateBestSolution(betterSolution, WeightSelection.Accepted);
        assertEquals(WeightSelection.NewGlobalBest, result, "Better solution should update best solution and return NewGlobalBest");
        try {
            java.lang.reflect.Field bestSolutionField = ParallelAlns.class.getDeclaredField("BestSolution");
            bestSolutionField.setAccessible(true);
            KnapsackSolution newBest = (KnapsackSolution) bestSolutionField.get(alns);
            assertEquals(betterSolution.getObjective(), newBest.getObjective(), PRECISION, "Best solution should be updated to better solution");
        } catch (Exception e) {
            fail("Failed to get BestSolution field: " + e.getMessage());
        }

        // Test with worse solution
        result = alns.UpdateBestSolution(worseSolution, WeightSelection.Accepted);
        assertEquals(WeightSelection.Accepted, result, "Worse solution should not update best solution");
        try {
            java.lang.reflect.Field bestSolutionField = ParallelAlns.class.getDeclaredField("BestSolution");
            bestSolutionField.setAccessible(true);
            KnapsackSolution newBest = (KnapsackSolution) bestSolutionField.get(alns);
            assertEquals(betterSolution.getObjective(), newBest.getObjective(), PRECISION, "Best solution should remain unchanged");
        } catch (Exception e) {
            fail("Failed to get BestSolution field: " + e.getMessage());
        }
    }

    @Test
    void testSelectOperatorIndex() {
        // Create test cumulative weights
        List<Double> cumulativeWeights = new ArrayList<>();
        cumulativeWeights.add(0.3);  // 0-0.3
        cumulativeWeights.add(0.6);  // 0.3-0.6
        cumulativeWeights.add(0.8);  // 0.6-0.8
        cumulativeWeights.add(1.0);  // 0.8-1.0

        // Set random seed for deterministic testing
        try {
            java.lang.reflect.Field randomizerField = ParallelAlns.class.getDeclaredField("_randomizer");
            randomizerField.setAccessible(true);
            randomizerField.set(alns, new Random(42));
        } catch (Exception e) {
            fail("Failed to set _randomizer field: " + e.getMessage());
        }

        // Test multiple selections
        int[] selections = new int[1000];
        for (int i = 0; i < 1000; i++) {
            int index = alns.SelectOperatorIndex(cumulativeWeights);
            assertTrue(index >= 0 && index < cumulativeWeights.size(), 
                "Selected index should be within bounds");
            selections[index]++;
        }

        // Verify distribution (roughly)
        // Note: This is a probabilistic test, so we use wide bounds
        for (int i = 0; i < cumulativeWeights.size(); i++) {
            double probability = (i == 0 ? cumulativeWeights.get(i) : 
                cumulativeWeights.get(i) - cumulativeWeights.get(i-1));
            double expected = 1000 * probability;
            double tolerance = expected * 0.2; // 20% tolerance
            assertTrue(Math.abs(selections[i] - expected) <= tolerance,
                "Selection distribution should roughly match cumulative weights");
        }
    }
} 
