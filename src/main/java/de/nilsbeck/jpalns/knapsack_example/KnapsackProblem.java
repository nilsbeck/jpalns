package de.nilsbeck.jpalns.knapsack_example;

import java.util.List;

/**
 * Represents a knapsack problem with a list of items and a capacity.
 */
public class KnapsackProblem {
    private final List<Item> items;
    private final int capacity;

    public KnapsackProblem(List<Item> items, int capacity) {
        this.items = items;
        this.capacity = capacity;
    }

    public List<Item> getItems() {
        return items;
    }

    public int getCapacity() {
        return capacity;
    }

    public static class Item {
        private final int weight;
        private final int value;
        private final String id;

        public Item(String id, int weight, int value) {
            this.weight = weight;
            this.value = value;
            this.id = id;
        }

        public int getWeight() {
            return weight;
        }

        public int getValue() {
            return value;
        }

        public String getId() {
            return id;
        }
    }
} 
