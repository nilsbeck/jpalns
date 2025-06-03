package de.nilsbeck.job_scheduling_example;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

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
    private Duration makespan;
    private final Map<String, List<ScheduledJob>> machineAssignments;
    private final int totalJobs;
    private static final double PENALTY_PER_UNASSIGNED_JOB = 1000000.0; // Large penalty to ensure all jobs are scheduled

    /**
     * Creates an empty solution with no scheduled jobs.
     * Makespan is zero since there are no jobs to schedule.
     * @param totalJobs Total number of jobs that should be scheduled
     */
    public JobSchedulingSolution(int totalJobs) {
        this.scheduledJobs = new ArrayList<>();
        this.makespan = Duration.ZERO;  // No jobs = no time needed
        this.machineAssignments = new HashMap<>();
        this.totalJobs = totalJobs;
    }

    /**
     * Creates a solution with scheduled jobs.
     * @param scheduledJobs List of scheduled jobs
     * @param makespan Total duration from start of first job to end of last job
     * @param machineAssignments Map of machine IDs to their assigned jobs
     * @param totalJobs Total number of jobs that should be scheduled
     * @throws IllegalArgumentException if makespan is negative or if makespan is zero for non-empty schedule
     */
    public JobSchedulingSolution(List<ScheduledJob> scheduledJobs, Duration makespan, Map<String, List<ScheduledJob>> machineAssignments, int totalJobs) {
        if (makespan.isNegative()) {
            throw new IllegalArgumentException("Makespan cannot be negative");
        }
        if (scheduledJobs.isEmpty() && !makespan.isZero()) {
            throw new IllegalArgumentException("Empty schedule must have zero makespan");
        }
        if (!scheduledJobs.isEmpty() && makespan.isZero()) {
            throw new IllegalArgumentException("Non-empty schedule cannot have zero makespan");
        }
        if (scheduledJobs.size() > totalJobs) {
            throw new IllegalArgumentException("Cannot schedule more jobs than total available jobs");
        }
        
        this.scheduledJobs = new ArrayList<>(scheduledJobs);
        this.makespan = makespan;
        this.machineAssignments = new HashMap<>();
        // Create new lists for each machine's assignments
        for (Map.Entry<String, List<ScheduledJob>> entry : machineAssignments.entrySet()) {
            this.machineAssignments.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.totalJobs = totalJobs;
    }

    /**
     * Updates the makespan of the solution.
     * This should be called whenever jobs are added or removed.
     */
    public void updateMakespan() {
        if (scheduledJobs.isEmpty()) {
            this.makespan = Duration.ZERO;
            return;
        }
        
        Instant earliestStart = scheduledJobs.stream()
            .map(ScheduledJob::startTime)
            .min(Instant::compareTo)
            .orElseThrow();
            
        Instant latestEnd = scheduledJobs.stream()
            .map(ScheduledJob::endTime)
            .max(Instant::compareTo)
            .orElseThrow();
            
        this.makespan = Duration.between(earliestStart, latestEnd);
    }

    /**
     * Adds a job to the solution and updates the makespan.
     * @param job The job to add
     */
    public void addJob(ScheduledJob job) {
        scheduledJobs.add(job);
        machineAssignments.computeIfAbsent(job.machine().id(), k -> new ArrayList<>())
            .add(job);
        updateMakespan();
    }

    /**
     * Removes a job from the solution and updates the makespan.
     * @param job The job to remove
     * @return true if the job was removed, false if it wasn't found
     */
    public boolean removeJob(ScheduledJob job) {
        boolean removed = scheduledJobs.remove(job);
        if (removed) {
            List<ScheduledJob> machineJobs = machineAssignments.get(job.machine().id());
            if (machineJobs != null) {
                machineJobs.remove(job);
                if (machineJobs.isEmpty()) {
                    machineAssignments.remove(job.machine().id());
                }
            }
            updateMakespan();
        }
        return removed;
    }

    /**
     * Removes all jobs from the solution.
     */
    public void clearJobs() {
        scheduledJobs.clear();
        machineAssignments.clear();
        this.makespan = Duration.ZERO;
    }

    @Override
    public double getObjective() {
        // Calculate penalty for unassigned jobs
        int unassignedJobs = totalJobs - scheduledJobs.size();
        double penalty = unassignedJobs * PENALTY_PER_UNASSIGNED_JOB;
        
        // Return makespan plus penalty
        // The penalty ensures that partial solutions are always worse than complete solutions
        return makespan.toMinutes() + penalty;
    }

    @Override
    public JobSchedulingSolution Clone() {
        // Create new lists and maps with the same content
        // Since ScheduledJob is a record (immutable), we can reuse the instances
        List<ScheduledJob> clonedScheduledJobs = new ArrayList<>(scheduledJobs);
        
        // Create new lists for each machine's assignments
        Map<String, List<ScheduledJob>> clonedMachineAssignments = new HashMap<>();
        for (Map.Entry<String, List<ScheduledJob>> entry : machineAssignments.entrySet()) {
            clonedMachineAssignments.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        
        // Create new solution with cloned collections
        // Duration is immutable, so we can reuse it
        return new JobSchedulingSolution(
            clonedScheduledJobs,
            makespan,
            clonedMachineAssignments,
            totalJobs
        );
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

    public int getTotalJobs() {
        return totalJobs;
    }

    public int getUnassignedJobs() {
        return totalJobs - scheduledJobs.size();
    }
} 
