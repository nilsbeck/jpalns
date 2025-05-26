package de.nilsbeck.jpalns;

/**
 * Every solution type must implement this interface
 * @param <T> The type of the solution
 */
public interface ISolution<T> extends IPalnsClonable<T> {
        double getObjective();
    }
