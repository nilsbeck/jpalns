package de.nilsbeck.knapsack_example;

import java.util.ArrayList;
import java.util.List;

/**
 * Main class for the knapsack problem example.
 * Defines a static knapsack problem (items and capacity) and solves it using the ALNS solver.
 */
public class KnapsackExample {
    public static void main(String[] args) {
        // Create a sample knapsack problem
        // Example from https://developers.google.com/optimization/pack/knapsack
        List<KnapsackProblem.Item> items = new ArrayList<>();
        int[] weights = {7, 0, 30, 22, 80, 94, 11, 81, 70, 64, 59, 18, 0, 36, 3, 8, 15, 42, 9, 0, 42, 47, 52, 32, 26, 48, 55, 6, 29, 84, 2, 4, 18, 56, 7, 29, 93, 44, 71, 3, 86, 66, 31, 65, 0, 79, 20, 65, 52, 13};
        int[] values = {360, 83, 59, 130, 431, 67, 230, 52, 93, 125, 670, 892, 600, 38, 48, 147, 78, 256, 63, 17, 120, 164, 432, 35, 92, 110, 22, 42, 50, 323, 514, 28, 87, 73, 78, 15, 26, 78, 210, 36, 85, 189, 274, 43, 33, 10, 19, 389, 276, 312};
        for (int i = 0; i < weights.length; i++) {
            items.add(new KnapsackProblem.Item("Item" + (i + 1), weights[i], values[i]));
        }
        
        KnapsackProblem problem = new KnapsackProblem(items, 850);  // capacity = 850
        
        // Create and configure the solver
        KnapsackSolver solver = new KnapsackSolver(
            10,    // number of iterations
            42       // random seed
        );
        
        // Solve the problem
        solver.Solve(problem);
    }
} 
