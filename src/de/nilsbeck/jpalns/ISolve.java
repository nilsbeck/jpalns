package de.nilsbeck.jpalns;

/// <summary>
/// A basic solver interface
/// </summary>
/// <typeparam name="TInput">The type of the problem input</typeparam>
/// <typeparam name="TOutput">The type of the solution output</typeparam>
interface ISolve<TInput, TOutput extends  ISolution<TOutput>>
    {
        TOutput Solve(TInput input);
    }
