package de.nilsbeck.job_scheduling_example.model;

import java.util.List;

public record ModelInput(
    List<Machine> machines,
    List<Job> jobs
) {} 
