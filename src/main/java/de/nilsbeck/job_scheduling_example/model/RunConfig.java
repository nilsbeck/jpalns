package de.nilsbeck.job_scheduling_example.model;

public record RunConfig(
    Run run
) {
    public record Run(
        String name
    ) {}
} 
