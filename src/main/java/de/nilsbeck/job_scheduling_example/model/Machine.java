package de.nilsbeck.job_scheduling_example.model;

import java.time.Instant;

public record Machine(
    String id,
    Instant start
) {} 
