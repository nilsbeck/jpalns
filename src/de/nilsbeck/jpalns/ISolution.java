package de.nilsbeck.jpalns;

/// <summary>
/// Every solution type must implement this interface
/// </summary>
/// <typeparam name="T">The type of the solution</typeparam>
public interface ISolution<T> extends IPalnsClonable<T> {
        double getObjective();
    }
