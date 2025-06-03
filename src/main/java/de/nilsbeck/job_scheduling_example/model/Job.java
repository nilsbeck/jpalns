package de.nilsbeck.job_scheduling_example.model;

import java.time.Duration;
import java.util.List;

public record Job(
    String id,
    List<String> requiredResources,
    Duration duration,
    List<JobDependency> dependencies
) {} 
