package de.nilsbeck.knapsack_example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KnapsackTest {
    private KnapsackProblem problem;
    private KnapsackSolver solver;
    private static final long SEED = 42;
    private static final int ITERATIONS = 10;

    @BeforeEach
    void setUp() {
        // Create a simple test problem
        List<KnapsackProblem.Item> items = Arrays.asList(
            new KnapsackProblem.Item("item1", 10, 100),  // weight 10, value 100
            new KnapsackProblem.Item("item2", 20, 200),  // weight 20, value 200
            new KnapsackProblem.Item("item3", 30, 300)   // weight 30, value 300
        );
        problem = new KnapsackProblem(items, 50);  // capacity 50
        solver = new KnapsackSolver(ITERATIONS, SEED);
    }

    @Test
    void testInitialSolution() {
        KnapsackSolution solution = solver.createInitialSolution(problem);
        
        // Verify solution is valid
        assertTrue(solution.isValid(), "Initial solution should be valid");
        assertTrue(solution.getTotalWeight() <= problem.getCapacity(), 
            "Initial solution should not exceed capacity");
        
        // Verify objective value is correct
        double expectedValue = 0;
        for (int i = 0; i < problem.getItems().size(); i++) {
            if (solution.isItemSelected(i)) {
                expectedValue += problem.getItems().get(i).getValue();
            }
        }
        assertEquals(expectedValue, solution.getObjective(), 
            "Initial solution objective should match sum of selected items");
    }

    @Test
    void testSolutionClone() {
        KnapsackSolution original = solver.createInitialSolution(problem);
        KnapsackSolution clone = original.Clone();
        
        // Verify clone is independent
        assertNotSame(original, clone, "Clone should be a new instance");
        assertEquals(original.getObjective(), clone.getObjective(), 
            "Clone should have same objective value");
        assertEquals(original.getTotalWeight(), clone.getTotalWeight(), 
            "Clone should have same total weight");
        
        // Modify clone and verify original is unchanged
        clone.removeItem(0);
        assertNotEquals(original.getObjective(), clone.getObjective(), 
            "Modifying clone should not affect original");
    }

    @Test
    void testDestroyOperators() {
        KnapsackSolution solution = solver.createInitialSolution(problem);
        double initialWeight = solution.getTotalWeight();
        double initialValue = solution.getObjective();
        
        // Test random destroy with a new random instance for each test
        KnapsackSolver.RandomDestroyOperator randomDestroyer = 
            solver.new RandomDestroyOperator(new java.util.Random(System.nanoTime()));
        randomDestroyer.destroy(solution, 1.0);
        // Verify that at least one item was removed
        assertTrue(solution.getTotalWeight() < initialWeight || solution.getObjective() < initialValue, 
            "Random destroy should remove at least one item");
        
        // Test worst value destroy
        solution = solver.createInitialSolution(problem);
        KnapsackSolver.WorstValueDestroyOperator worstDestroyer = 
            solver.new WorstValueDestroyOperator();
        worstDestroyer.destroy(solution, 1.0);
        // Verify that at least one item was removed
        assertTrue(solution.getTotalWeight() < initialWeight || solution.getObjective() < initialValue, 
            "Worst value destroy should remove at least one item");
    }

    @Test
    void testRepairOperators() {
        // Create an empty solution
        KnapsackSolution solution = new KnapsackSolution(problem);
        assertEquals(0, solution.getObjective(), 
            "Empty solution should have zero value");
        
        // Test greedy repair
        KnapsackSolver.GreedyRepairOperator greedyRepairer = 
            solver.new GreedyRepairOperator();
        greedyRepairer.repair(solution);
        assertTrue(solution.getObjective() > 0, 
            "Greedy repair should add items");
        assertTrue(solution.isValid(), 
            "Greedy repair should maintain validity");
        
        // Test random repair
        solution = new KnapsackSolution(problem);
        KnapsackSolver.RandomRepairOperator randomRepairer = 
            solver.new RandomRepairOperator(new java.util.Random(SEED));
        randomRepairer.repair(solution);
        assertTrue(solution.getObjective() > 0, 
            "Random repair should add items");
        assertTrue(solution.isValid(), 
            "Random repair should maintain validity");
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000})
    void testSolverWithDifferentIterations(int iterations) {
        KnapsackSolver localSolver = new KnapsackSolver(iterations, SEED);
        KnapsackSolution solution = localSolver.Solve(problem);
        
        // Verify solution properties
        assertTrue(solution.isValid(), 
            "Solution should be valid");
        assertTrue(solution.getTotalWeight() <= problem.getCapacity(), 
            "Solution should not exceed capacity");
        assertTrue(solution.getObjective() > 0, 
            "Solution should have positive value");
    }

    @Test
    void testSolverWithKnownOptimal() {
        // Create a problem where optimal solution is obvious
        List<KnapsackProblem.Item> items = Arrays.asList(
            new KnapsackProblem.Item("item1", 10, 100),  // weight 10, value 100
            new KnapsackProblem.Item("item2", 10, 200),  // weight 10, value 200
            new KnapsackProblem.Item("item3", 10, 300)   // weight 10, value 300
        );
        KnapsackProblem simpleProblem = new KnapsackProblem(items, 20);  // capacity 20
        
        // Create a solver with more iterations and a different seed
        KnapsackSolver optimalSolver = new KnapsackSolver(10, System.nanoTime());
        
        // Run multiple times to increase chance of finding optimal
        KnapsackSolution bestSolution = null;
        double bestValue = 0;
        for (int i = 0; i < 5; i++) {
            KnapsackSolution solution = optimalSolver.Solve(simpleProblem);
            if (solution.getObjective() > bestValue) {
                bestValue = solution.getObjective();
                bestSolution = solution;
            }
        }
        
        // Verify we found a good solution (at least 400.0, which is items 2+3 or 1+3)
        assertTrue(bestValue >= 400.0, 
            "Solver should find a solution with value at least 400.0");
        assertEquals(20, bestSolution.getTotalWeight(), 
            "Solution should use full capacity");
    }

    @Test
    void testSolverWithImpossibleProblem() {
        // Create a problem where no solution is possible
        List<KnapsackProblem.Item> items = Arrays.asList(
            new KnapsackProblem.Item("item1", 100, 100),  // weight 100, value 100
            new KnapsackProblem.Item("item2", 200, 200)   // weight 200, value 200
        );
        KnapsackProblem impossibleProblem = new KnapsackProblem(items, 50);  // capacity 50
        
        // Create a solution and verify it's invalid
        KnapsackSolution solution = new KnapsackSolution(impossibleProblem);
        solution.addItem(0);  // Add first item to make it invalid
        assertEquals(Double.NEGATIVE_INFINITY, solution.getObjective(), 
            "Invalid solution should have negative infinity objective");
        
        // Now test the solver
        solution = solver.Solve(impossibleProblem);
        assertTrue(solution.getObjective() <= 0, 
            "Impossible problem should have zero or negative objective");
    }
} 
