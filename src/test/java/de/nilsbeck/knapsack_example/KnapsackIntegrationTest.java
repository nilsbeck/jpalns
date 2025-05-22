package de.nilsbeck.knapsack_example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class KnapsackIntegrationTest {
    
    @Test
    public void testKnapsackSolverOutput() {
        // Redirect System.out to capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // Create a test problem with known values
            List<KnapsackProblem.Item> items = new ArrayList<>();
            items.add(new KnapsackProblem.Item("item1", 12, 4));  // weight, value
            items.add(new KnapsackProblem.Item("item2", 2, 2));
            items.add(new KnapsackProblem.Item("item3", 1, 2));
            items.add(new KnapsackProblem.Item("item4", 1, 1));
            items.add(new KnapsackProblem.Item("item5", 4, 10));
            KnapsackProblem problem = new KnapsackProblem(items, 15);

            // Create and run solver with 10 iterations
            KnapsackSolver solver = new KnapsackSolver(10, 42); // Using fixed seed for reproducibility
            KnapsackSolution solution = solver.Solve(problem);

            // Get the output
            String output = outContent.toString();

            // Verify the output contains the expected iteration result
            assertTrue(output.contains("Iteration: 10, Best value: 7534.0"),
                "Expected output not found. Actual output:\n" + output);

            // Additional assertions to verify solution quality
            assertTrue(solution.getObjective() > 0, "Solution should have positive value");
            assertTrue(solution.getTotalWeight() <= problem.getCapacity(), 
                "Solution weight should not exceed capacity");

        } finally {
            // Restore original System.out
            System.setOut(originalOut);
        }
    }
} 
