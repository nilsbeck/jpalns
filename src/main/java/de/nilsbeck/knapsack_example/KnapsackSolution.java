package de.nilsbeck.knapsack_example;

import de.nilsbeck.jpalns.ISolution;
import java.util.BitSet;

/**
 * Represents a solution to the knapsack problem.
 * Implements the ISolution interface.
 * Implements the Cloneable interface.
 */
public class KnapsackSolution implements ISolution<KnapsackSolution> {
    private final BitSet selectedItems;
    private final KnapsackProblem problem;
    private double objectiveValue;
    private int totalWeight;

    public KnapsackSolution(KnapsackProblem problem) {
        this.problem = problem;
        this.selectedItems = new BitSet(problem.getItems().size());
        this.objectiveValue = 0;
        this.totalWeight = 0;
    }

    public void addItem(int index) {
        if (!selectedItems.get(index)) {
            KnapsackProblem.Item item = problem.getItems().get(index);
            selectedItems.set(index);
            objectiveValue += item.getValue();
            totalWeight += item.getWeight();
        }
    }

    public void removeItem(int index) {
        if (selectedItems.get(index)) {
            KnapsackProblem.Item item = problem.getItems().get(index);
            selectedItems.clear(index);
            objectiveValue -= item.getValue();
            totalWeight -= item.getWeight();
        }
    }

    public boolean isItemSelected(int index) {
        return selectedItems.get(index);
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public boolean isValid() {
        return totalWeight <= problem.getCapacity();
    }

    public KnapsackProblem getProblem() {
        return problem;
    }

    @Override
    public double getObjective() {
        return isValid() ? objectiveValue : Double.NEGATIVE_INFINITY;
    }

    @Override
    public KnapsackSolution Clone() {
        KnapsackSolution clone = new KnapsackSolution(problem);
        clone.selectedItems.or(this.selectedItems);
        clone.objectiveValue = this.objectiveValue;
        clone.totalWeight = this.totalWeight;
        return clone;
    }
} 
