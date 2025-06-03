package de.nilsbeck.job_scheduling_example;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import de.nilsbeck.job_scheduling_example.model.Job;
import de.nilsbeck.job_scheduling_example.model.JobDependency;
import de.nilsbeck.job_scheduling_example.model.Machine;
import de.nilsbeck.job_scheduling_example.model.ModelInput;

public class JobSchedulingSolverValidationTest {

    @Test
    void testValidSolution() {
        // Create test data
        Machine machine1 = new Machine("M1", Instant.EPOCH);
        Machine machine2 = new Machine("M2", Instant.EPOCH);
        
        Job job1 = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        Job job2 = new Job("J2", List.of("M2"), Duration.ofHours(2), List.of(new JobDependency("J1")));
        
        ModelInput input = new ModelInput(
            List.of(machine1, machine2),
            List.of(job1, job2)
        );
        
        // Create a valid solution
        Instant start1 = Instant.EPOCH;
        Instant end1 = start1.plus(job1.duration());
        Instant start2 = end1; // Job2 starts when Job1 ends
        Instant end2 = start2.plus(job2.duration());
        
        JobSchedulingSolution.ScheduledJob scheduledJob1 = new JobSchedulingSolution.ScheduledJob(
            job1, machine1, start1, end1);
        JobSchedulingSolution.ScheduledJob scheduledJob2 = new JobSchedulingSolution.ScheduledJob(
            job2, machine2, start2, end2);
            
        JobSchedulingSolution solution = new JobSchedulingSolution(
            List.of(scheduledJob1, scheduledJob2),
            Duration.between(start1, end2),
            Map.of(
                "M1", List.of(scheduledJob1),
                "M2", List.of(scheduledJob2)
            ),
            2
        );
        
        // Validate
        var validation = JobSchedulingSolver.validateSolution(solution, input);
        if (!validation.isValid()) {
            fail("Validation errors: " + validation.errors());
        }
        assertTrue(validation.isValid(), "Solution should be valid");
        assertTrue(validation.errors().isEmpty(), "Should have no validation errors");
    }
    
    @Test
    void testInvalidSolutionWithOverlappingJobs() {
        // Create test data
        Machine machine = new Machine("M1", Instant.EPOCH);
        Job job1 = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        Job job2 = new Job("J2", List.of("M1"), Duration.ofHours(2), List.of());
        
        ModelInput input = new ModelInput(
            List.of(machine),
            List.of(job1, job2)
        );
        
        // Create an invalid solution with overlapping jobs
        Instant start1 = Instant.EPOCH;
        Instant end1 = start1.plus(job1.duration());
        Instant start2 = start1.plus(Duration.ofMinutes(30)); // Overlaps with job1
        Instant end2 = start2.plus(job2.duration());
        
        JobSchedulingSolution.ScheduledJob scheduledJob1 = new JobSchedulingSolution.ScheduledJob(
            job1, machine, start1, end1);
        JobSchedulingSolution.ScheduledJob scheduledJob2 = new JobSchedulingSolution.ScheduledJob(
            job2, machine, start2, end2);
            
        JobSchedulingSolution solution = new JobSchedulingSolution(
            List.of(scheduledJob1, scheduledJob2),
            Duration.between(start1, end2),
            Map.of("M1", List.of(scheduledJob1, scheduledJob2)),
            2
        );
        
        // Validate
        var validation = JobSchedulingSolver.validateSolution(solution, input);
        assertFalse(validation.isValid(), "Solution should be invalid");
        assertTrue(validation.errors().stream()
            .anyMatch(error -> error.contains("overlap")), 
            "Should have overlap error");
    }
    
    @Test
    void testInvalidSolutionWithDependencyViolation() {
        // Create test data
        Machine machine = new Machine("M1", Instant.EPOCH);
        Job job1 = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        Job job2 = new Job("J2", List.of("M1"), Duration.ofHours(2), List.of(new JobDependency("J1")));
        
        ModelInput input = new ModelInput(
            List.of(machine),
            List.of(job1, job2)
        );
        
        // Create an invalid solution where job2 starts before job1 finishes
        Instant start1 = Instant.EPOCH;
        Instant end1 = start1.plus(job1.duration());
        Instant start2 = start1.plus(Duration.ofMinutes(30)); // Starts before job1 finishes
        Instant end2 = start2.plus(job2.duration());
        
        JobSchedulingSolution.ScheduledJob scheduledJob1 = new JobSchedulingSolution.ScheduledJob(
            job1, machine, start1, end1);
        JobSchedulingSolution.ScheduledJob scheduledJob2 = new JobSchedulingSolution.ScheduledJob(
            job2, machine, start2, end2);
            
        JobSchedulingSolution solution = new JobSchedulingSolution(
            List.of(scheduledJob1, scheduledJob2),
            Duration.between(start1, end2),
            Map.of("M1", List.of(scheduledJob1, scheduledJob2)),
            2
        );
        
        // Validate
        var validation = JobSchedulingSolver.validateSolution(solution, input);
        assertFalse(validation.isValid(), "Solution should be invalid");
        assertTrue(validation.errors().stream()
            .anyMatch(error -> error.contains("starts before its dependency")), 
            "Should have dependency violation error");
    }
    
    @Test
    void testInvalidSolutionWithWrongMachine() {
        // Create test data
        Machine machine1 = new Machine("M1", Instant.EPOCH);
        Machine machine2 = new Machine("M2", Instant.EPOCH);
        Job job = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of()); // Requires M1
        
        ModelInput input = new ModelInput(
            List.of(machine1, machine2),
            List.of(job)
        );
        
        // Create an invalid solution where job is assigned to wrong machine
        Instant start = Instant.EPOCH;
        Instant end = start.plus(job.duration());
        
        JobSchedulingSolution.ScheduledJob scheduledJob = new JobSchedulingSolution.ScheduledJob(
            job, machine2, start, end); // Assigned to M2 instead of M1
            
        JobSchedulingSolution solution = new JobSchedulingSolution(
            List.of(scheduledJob),
            Duration.between(start, end),
            Map.of("M2", List.of(scheduledJob)),
            1
        );
        
        // Validate
        var validation = JobSchedulingSolver.validateSolution(solution, input);
        assertFalse(validation.isValid(), "Solution should be invalid");
        assertTrue(validation.errors().stream()
            .anyMatch(error -> error.contains("incorrect machine")), 
            "Should have wrong machine error");
    }
} 
