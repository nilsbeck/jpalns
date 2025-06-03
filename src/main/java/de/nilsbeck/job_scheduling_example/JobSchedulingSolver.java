package de.nilsbeck.job_scheduling_example;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.nilsbeck.job_scheduling_example.model.Job;
import de.nilsbeck.job_scheduling_example.model.JobDependency;
import de.nilsbeck.job_scheduling_example.model.Machine;
import de.nilsbeck.job_scheduling_example.model.ModelInput;
import de.nilsbeck.jpalns.ISolve;
import de.nilsbeck.jpalns.ParallelAlns;
import de.nilsbeck.jpalns.Sense;

/**
 * Solver for the job scheduling problem.
 * Jobs form a DAG where some jobs have no dependencies (root nodes)
 * and other jobs depend on previous jobs in the sequence.
 */
public class JobSchedulingSolver implements ISolve<ModelInput, JobSchedulingSolution> {
    
    private final Random random;
    private final int maxIterations;
    private final AtomicInteger iteration = new AtomicInteger(0);
    private static final int REPORT_INTERVAL = 1; // Report every iteration

    // ALNS parameters
    private static final double TEMPERATURE = 1000.0;
    private static final double ALPHA = 0.99;
    private static final double NEW_GLOBAL_BEST_WEIGHT = 33.0;
    private static final double BETTER_SOLUTION_WEIGHT = 20.0;
    private static final double ACCEPTED_SOLUTION_WEIGHT = 10.0;
    private static final double REJECTED_SOLUTION_WEIGHT = 0.0;
    private static final double DECAY = 0.999;
    private static final double INITIAL_WEIGHT = 1.0;
    private static final double PRECISION = 1e-5;

    public JobSchedulingSolver(int numIterations, long seed) {
        this.random = new Random(seed);
        this.maxIterations = numIterations;
    }

    @Override
    public JobSchedulingSolution Solve(ModelInput input) {
        // Create destroy and repair operators
        ArrayList<Function<JobSchedulingSolution, CompletableFuture<JobSchedulingSolution>>> destroyOperators = createDestroyOperators();
        ArrayList<Function<JobSchedulingSolution, CompletableFuture<JobSchedulingSolution>>> repairOperators = createRepairOperators();

        // Create and configure ALNS solver with explicit type parameters
        var alns = new ParallelAlns<ModelInput, JobSchedulingSolution>(
            createInitialSolution(),
            destroyOperators,
            repairOperators,
            TEMPERATURE,
            ALPHA,
            random,
            NEW_GLOBAL_BEST_WEIGHT,
            BETTER_SOLUTION_WEIGHT,
            ACCEPTED_SOLUTION_WEIGHT,
            REJECTED_SOLUTION_WEIGHT,
            DECAY,
            INITIAL_WEIGHT,
            PRECISION,
            1,  // numberOfThreads - start with single thread for now
            Sense.MINIMIZE,  // We want to minimize makespan
            solution -> iteration.get() >= maxIterations,  // abort function
            createProgressCallback()  // progress update callback
        );

        // Run the solver
        return alns.Solve(input);
    }

    private Function<ModelInput, JobSchedulingSolution> createInitialSolution() {
        return input -> {
            // 1. Get jobs in topological order (respecting dependencies)
            List<Job> orderedJobs = getTopologicalOrder(input.jobs());
            
            // 2. Create a map of machine IDs to their start times
            Map<String, Instant> machineAvailableFrom = new HashMap<>();
            for (Machine machine : input.machines()) {
                machineAvailableFrom.put(machine.id(), machine.start());
            }
            
            // 3. Schedule jobs in order
            List<JobSchedulingSolution.ScheduledJob> scheduledJobs = new ArrayList<>();
            Map<String, List<JobSchedulingSolution.ScheduledJob>> machineAssignments = new HashMap<>();
            
            for (Job job : orderedJobs) {
                // Find earliest possible start time considering:
                // a) Machine availability
                // b) Job dependencies
                Instant startTime = findEarliestStartTime(job, machineAvailableFrom, scheduledJobs);
                
                // Schedule job on its required machine
                String machineId = job.requiredResources().get(0); // Assuming one machine per job for now
                Instant endTime = startTime.plus(job.duration());
                
                // Update machine availability
                machineAvailableFrom.put(machineId, endTime);
                
                // Create scheduled job
                Machine machine = input.machines().stream()
                    .filter(m -> m.id().equals(machineId))
                    .findFirst()
                    .orElseThrow();
                    
                JobSchedulingSolution.ScheduledJob scheduledJob = new JobSchedulingSolution.ScheduledJob(
                    job, machine, startTime, endTime);
                
                // Add to our tracking structures
                scheduledJobs.add(scheduledJob);
                machineAssignments.computeIfAbsent(machineId, k -> new ArrayList<>())
                    .add(scheduledJob);
            }
            
            // 4. Calculate makespan (time from start to finish of last job)
            Duration makespan = calculateMakespan(scheduledJobs);
            
            return new JobSchedulingSolution(scheduledJobs, makespan, machineAssignments, input.jobs().size());
        };
    }

    /**
     * Returns jobs in topological order, with root nodes (no dependencies) first
     */
    private List<Job> getTopologicalOrder(List<Job> jobs) {
        // Create a map of job ID to job for quick lookup
        Map<String, Job> jobMap = jobs.stream()
            .collect(Collectors.toMap(Job::id, job -> job));
            
        // Create a map of job ID to its dependencies
        Map<String, Set<String>> dependencies = new HashMap<>();
        for (Job job : jobs) {
            Set<String> deps = job.dependencies().stream()
                .map(JobDependency::id)
                .collect(Collectors.toSet());
            dependencies.put(job.id(), deps);
        }
        
        // Find root nodes (jobs with no dependencies)
        Queue<Job> queue = new LinkedList<>();
        for (Job job : jobs) {
            if (job.dependencies().isEmpty()) {
                queue.add(job);
            }
        }
        
        // Perform topological sort
        List<Job> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        while (!queue.isEmpty()) {
            Job current = queue.poll();
            if (visited.add(current.id())) {
                result.add(current);
                
                // Add jobs that depend on current job
                for (Job job : jobs) {
                    if (job.dependencies().stream()
                            .map(JobDependency::id)
                            .anyMatch(id -> id.equals(current.id()))) {
                        // Remove current job from this job's dependencies
                        dependencies.get(job.id()).remove(current.id());
                        // If no more dependencies, add to queue
                        if (dependencies.get(job.id()).isEmpty()) {
                            queue.add(job);
                        }
                    }
                }
            }
        }
        
        if (result.size() != jobs.size()) {
            throw new IllegalStateException("Cycle detected in job dependencies");
        }
        
        return result;
    }

    /**
     * Finds the earliest possible start time for a job considering:
     * 1. Machine availability
     * 2. Job dependencies (must start after dependent jobs finish)
     */
    private Instant findEarliestStartTime(
        Job job,
        Map<String, Instant> machineAvailableFrom,
        List<JobSchedulingSolution.ScheduledJob> scheduledJobs) {
        
        // Start time must be after all dependent jobs finish
        Instant earliestStart = job.dependencies().stream()
            .map(JobDependency::id)
            .map(depId -> scheduledJobs.stream()
                .filter(sj -> sj.job().id().equals(depId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Dependent job not scheduled: " + depId))
                .endTime())
            .max(Instant::compareTo)
            .orElse(Instant.EPOCH);  // If no dependencies, can start at EPOCH
            
        // Must also be after machine is available
        String machineId = job.requiredResources().get(0); // Assuming one machine per job
        Instant machineAvailable = machineAvailableFrom.get(machineId);
        
        return earliestStart.isAfter(machineAvailable) ? earliestStart : machineAvailable;
    }

    /**
     * Calculates the makespan (time from start to finish of last job)
     */
    private Duration calculateMakespan(List<JobSchedulingSolution.ScheduledJob> scheduledJobs) {
        if (scheduledJobs.isEmpty()) {
            return Duration.ZERO;
        }
        
        Instant earliestStart = scheduledJobs.stream()
            .map(JobSchedulingSolution.ScheduledJob::startTime)
            .min(Instant::compareTo)
            .orElseThrow();
            
        Instant latestEnd = scheduledJobs.stream()
            .map(JobSchedulingSolution.ScheduledJob::endTime)
            .max(Instant::compareTo)
            .orElseThrow();
            
        return Duration.between(earliestStart, latestEnd);
    }

    private ArrayList<Function<JobSchedulingSolution, CompletableFuture<JobSchedulingSolution>>> createDestroyOperators() {
        ArrayList<Function<JobSchedulingSolution, CompletableFuture<JobSchedulingSolution>>> operators = new ArrayList<>();
        
        // Add a destroy operator that removes 2-3 random jobs and their dependents
        operators.add(solution -> CompletableFuture.supplyAsync(() -> {
            // Create a clone of the solution to modify
            JobSchedulingSolution newSolution = solution.Clone();
            
            // Select 2-3 random jobs to remove
            int numJobsToRemove = random.nextInt(2) + 2; // Random number between 2 and 3
            List<JobSchedulingSolution.ScheduledJob> allJobs = new ArrayList<>(newSolution.getScheduledJobs());
            
            if (allJobs.size() <= numJobsToRemove) {
                // If we have fewer jobs than we want to remove, remove all
                newSolution.clearJobs();
                return newSolution;
            }
            
            // Randomly select jobs to remove
            Set<String> jobsToRemoveIds = new HashSet<>();
            for (int i = 0; i < numJobsToRemove; i++) {
                int index = random.nextInt(allJobs.size());
                JobSchedulingSolution.ScheduledJob job = allJobs.remove(index);
                jobsToRemoveIds.add(job.job().id());
            }
            
            // Find all dependent jobs that need to be removed
            for (JobSchedulingSolution.ScheduledJob job : new ArrayList<>(newSolution.getScheduledJobs())) {
                if (job.job().dependencies().stream()
                        .anyMatch(dep -> jobsToRemoveIds.contains(dep.id()))) {
                    jobsToRemoveIds.add(job.job().id());
                }
            }
            
            // Remove all selected jobs and their dependents
            for (JobSchedulingSolution.ScheduledJob job : new ArrayList<>(newSolution.getScheduledJobs())) {
                if (jobsToRemoveIds.contains(job.job().id())) {
                    newSolution.removeJob(job);
                }
            }
            
            return newSolution;
        }));
        
        return operators;
    }

    private ArrayList<Function<JobSchedulingSolution, CompletableFuture<JobSchedulingSolution>>> createRepairOperators() {
        ArrayList<Function<JobSchedulingSolution, CompletableFuture<JobSchedulingSolution>>> operators = new ArrayList<>();
        // Add a trivial repair operator (no-op)
        operators.add(solution -> CompletableFuture.completedFuture(solution));
        return operators;
    }

    private Consumer<JobSchedulingSolution> createProgressCallback() {
        return bestSolution -> {
            // Only increment if we haven't reached max iterations
            int current = iteration.get();
            if (current >= maxIterations) {
                return;
            }
            int currentIteration = iteration.incrementAndGet();
            
            // Only report if we haven't exceeded max iterations
            if (currentIteration <= maxIterations) {
                if (currentIteration % REPORT_INTERVAL == 0) {
                    System.out.println("Iteration: " + currentIteration + 
                        ", Best makespan: " + bestSolution.getMakespan().toMinutes() + " minutes" +
                        ", Unassigned jobs: " + bestSolution.getUnassignedJobs());
                }
                if (currentIteration == maxIterations) {
                    // Print final results
                    System.out.println("--------------------------------");
                    System.out.println("Best solution found:");
                    System.out.println("Makespan: " + bestSolution.getMakespan().toHours() + " hours");
                    System.out.println("Scheduled jobs: " + bestSolution.getScheduledJobs().size() + 
                        " of " + bestSolution.getTotalJobs());
                    System.out.println("Unassigned jobs: " + bestSolution.getUnassignedJobs());
                    System.out.println("--------------------------------");
                }
            }
        };
    }

    /**
     * Validates a solution for correctness and feasibility.
     * Checks:
     * 1. No job overlaps on the same machine
     * 2. Jobs are assigned to correct machines
     * 3. Job dependencies are respected (DAG)
     * 4. Makespan calculation is correct
     * 
     * @param solution The solution to validate
     * @param input The problem input containing job and machine definitions
     * @return A ValidationResult containing validation status and any error messages
     */
    public static ValidationResult validateSolution(JobSchedulingSolution solution, ModelInput input) {
        List<String> errors = new ArrayList<>();
        
        // Create lookup maps for efficiency
        Map<String, Job> jobMap = input.jobs().stream()
            .collect(Collectors.toMap(Job::id, job -> job));
        Map<String, Machine> machineMap = input.machines().stream()
            .collect(Collectors.toMap(Machine::id, machine -> machine));
            
        // 1. Check for job overlaps and correct machine assignments
        Map<String, List<JobSchedulingSolution.ScheduledJob>> machineAssignments = solution.getMachineAssignments();
        for (Map.Entry<String, List<JobSchedulingSolution.ScheduledJob>> entry : machineAssignments.entrySet()) {
            String machineId = entry.getKey();
            // Create a mutable copy of the jobs list for sorting
            List<JobSchedulingSolution.ScheduledJob> jobs = new ArrayList<>(entry.getValue());
            
            // Sort jobs by start time for efficient overlap checking
            jobs.sort(Comparator.comparing(JobSchedulingSolution.ScheduledJob::startTime));
            
            // Check each job against subsequent jobs for overlaps
            for (int i = 0; i < jobs.size(); i++) {
                JobSchedulingSolution.ScheduledJob job1 = jobs.get(i);
                
                // Verify machine assignment
                if (!job1.job().requiredResources().contains(machineId)) {
                    errors.add(String.format("Job %s assigned to incorrect machine %s", 
                        job1.job().id(), machineId));
                }
                
                // Check for overlaps with subsequent jobs
                for (int j = i + 1; j < jobs.size(); j++) {
                    JobSchedulingSolution.ScheduledJob job2 = jobs.get(j);
                    if (job1.endTime().isAfter(job2.startTime())) {
                        errors.add(String.format("Jobs %s and %s overlap on machine %s", 
                            job1.job().id(), job2.job().id(), machineId));
                    }
                }
            }
        }
        
        // 2. Check job dependencies and sequence
        Map<String, JobSchedulingSolution.ScheduledJob> scheduledJobMap = solution.getScheduledJobs().stream()
            .collect(Collectors.toMap(sj -> sj.job().id(), sj -> sj));
            
        for (JobSchedulingSolution.ScheduledJob scheduledJob : solution.getScheduledJobs()) {
            Job job = scheduledJob.job();
            
            // Check each dependency
            for (JobDependency dep : job.dependencies()) {
                JobSchedulingSolution.ScheduledJob depJob = scheduledJobMap.get(dep.id());
                if (depJob == null) {
                    errors.add(String.format("Job %s depends on job %s which is not scheduled", 
                        job.id(), dep.id()));
                } else if (depJob.endTime().isAfter(scheduledJob.startTime())) {
                    errors.add(String.format("Job %s starts before its dependency %s finishes", 
                        job.id(), dep.id()));
                }
            }
        }
        
        // 3. Verify makespan calculation
        if (!solution.getScheduledJobs().isEmpty()) {
            Instant earliestStart = solution.getScheduledJobs().stream()
                .map(JobSchedulingSolution.ScheduledJob::startTime)
                .min(Instant::compareTo)
                .orElseThrow();
                
            Instant latestEnd = solution.getScheduledJobs().stream()
                .map(JobSchedulingSolution.ScheduledJob::endTime)
                .max(Instant::compareTo)
                .orElseThrow();
                
            Duration calculatedMakespan = Duration.between(earliestStart, latestEnd);
            if (!calculatedMakespan.equals(solution.getMakespan())) {
                errors.add(String.format("Incorrect makespan calculation. Expected %s, got %s", 
                    calculatedMakespan, solution.getMakespan()));
            }
        } else if (!solution.getMakespan().isZero()) {
            errors.add("Empty solution should have zero makespan");
        }
        
        // 4. Check if all jobs are scheduled
        if (solution.getScheduledJobs().size() != solution.getTotalJobs()) {
            errors.add(String.format("Not all jobs scheduled. Expected %d, got %d", 
                solution.getTotalJobs(), solution.getScheduledJobs().size()));
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Result of solution validation
     */
    public record ValidationResult(
        boolean isValid,
        List<String> errors
    ) {
        public ValidationResult {
            errors = List.copyOf(errors); // Make errors list immutable
        }
        
        @Override
        public String toString() {
            if (isValid) {
                return "Solution is valid";
            }
            return "Solution is invalid:\n" + 
                errors.stream()
                    .map(error -> "- " + error)
                    .collect(Collectors.joining("\n"));
        }
    }
} 
