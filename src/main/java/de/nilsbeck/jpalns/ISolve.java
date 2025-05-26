package de.nilsbeck.jpalns;

/**
 * A basic solver interface
 * @param <TInput> The type of the problem input
 * @param <TOutput> The type of the solution output
 */
public interface ISolve<TInput, TOutput extends  ISolution<TOutput>>
    {
        TOutput Solve(TInput input);
    }
