package de.nilsbeck.knapsack_example;

import java.util.Arrays;
import java.util.List;

/**
 * Main class for the knapsack problem example.
 * Defines a static knapsack problem (items and capacity) and solves it using the ALNS solver.
 */
public class KnapsackExample {
    public static void main(String[] args) {
        // Create a sample knapsack problem
        List<KnapsackProblem.Item> items = Arrays.asList(
            new KnapsackProblem.Item("cat", 20, 100),  // weight, value
            new KnapsackProblem.Item("dog", 45, 20),
            new KnapsackProblem.Item("water", 2, 40),
            new KnapsackProblem.Item("phone", 1, 6),
            new KnapsackProblem.Item("book", 10, 63),
            new KnapsackProblem.Item("rx", 1, 81),
            new KnapsackProblem.Item("tablet", 8, 28),
            new KnapsackProblem.Item("coat", 9, 44),
            new KnapsackProblem.Item("laptop", 13, 51),
            new KnapsackProblem.Item("keys", 1, 92),
            new KnapsackProblem.Item("nuts", 4, 18)
        );
        
        KnapsackProblem problem = new KnapsackProblem(items, 50);  // capacity = 15
        
        // Create and configure the solver
        KnapsackSolver solver = new KnapsackSolver(
            1000,    // number of iterations
            42       // random seed
        );
        
        // Solve the problem
        solver.Solve(problem);
    }
} 
