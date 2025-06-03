package de.nilsbeck.job_scheduling_example;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import de.nilsbeck.job_scheduling_example.model.Job;
import de.nilsbeck.job_scheduling_example.model.Machine;
import de.nilsbeck.jpalns.ISolution;

/**
 * Represents a solution to the job scheduling problem.
 * This class implements ISolution to work with the ALNS framework.
 */
public class JobSchedulingSolution implements ISolution<JobSchedulingSolution> {
    
    /**
     * Represents a scheduled job with its start time and assigned machine
     */
    public record ScheduledJob(
        Job job,
        Machine machine,
        Instant startTime,
        Instant endTime
    ) {}

    private final List<ScheduledJob> scheduledJobs;
    private final Duration makespan;
    private final Map<String, List<ScheduledJob>> machineAssignments;

    public JobSchedulingSolution() {
        this.scheduledJobs = List.of();
        this.makespan = Duration.ZERO;
        this.machineAssignments = Map.of();
    }

    public JobSchedulingSolution(List<ScheduledJob> scheduledJobs, Duration makespan, Map<String, List<ScheduledJob>> machineAssignments) {
        this.scheduledJobs = scheduledJobs;
        this.makespan = makespan;
        this.machineAssignments = machineAssignments;
    }

    @Override
    public double getObjective() {
        // For job scheduling, we want to minimize the makespan
        // Return negative makespan since ISolution assumes maximization
        return -makespan.toMinutes();
    }

    @Override
    public JobSchedulingSolution Clone() {
        // TODO: Implement proper deep cloning
        return this;
    }

    public List<ScheduledJob> getScheduledJobs() {
        return scheduledJobs;
    }

    public Duration getMakespan() {
        return makespan;
    }

    public Map<String, List<ScheduledJob>> getMachineAssignments() {
        return machineAssignments;
    }
} 
