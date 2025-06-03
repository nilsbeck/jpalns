package de.nilsbeck.job_scheduling_example;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.nilsbeck.job_scheduling_example.model.Job;
import de.nilsbeck.job_scheduling_example.model.Machine;

public class JobSchedulingSolutionTest {

    @Test
    void testAddAndRemoveJob() {
        // Create test data
        Machine machine = new Machine("M1", Instant.EPOCH);
        Job job = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        
        // Create empty solution
        JobSchedulingSolution solution = new JobSchedulingSolution(1);
        assertEquals(0, solution.getScheduledJobs().size());
        assertEquals(Duration.ZERO, solution.getMakespan());
        assertTrue(solution.getMachineAssignments().isEmpty());
        
        // Add job
        Instant start = Instant.EPOCH;
        Instant end = start.plus(job.duration());
        JobSchedulingSolution.ScheduledJob scheduledJob = new JobSchedulingSolution.ScheduledJob(
            job, machine, start, end);
        solution.addJob(scheduledJob);
        
        // Verify job was added
        assertEquals(1, solution.getScheduledJobs().size());
        assertEquals(job.duration(), solution.getMakespan());
        assertEquals(1, solution.getMachineAssignments().size());
        assertEquals(1, solution.getMachineAssignments().get("M1").size());
        assertEquals(scheduledJob, solution.getMachineAssignments().get("M1").get(0));
        
        // Remove job
        assertTrue(solution.removeJob(scheduledJob));
        
        // Verify job was removed
        assertEquals(0, solution.getScheduledJobs().size());
        assertEquals(Duration.ZERO, solution.getMakespan());
        assertTrue(solution.getMachineAssignments().isEmpty());
    }
    
    @Test
    void testClearJobs() {
        // Create test data
        Machine machine1 = new Machine("M1", Instant.EPOCH);
        Machine machine2 = new Machine("M2", Instant.EPOCH);
        Job job1 = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        Job job2 = new Job("J2", List.of("M2"), Duration.ofHours(2), List.of());
        
        // Create solution with jobs
        Instant start1 = Instant.EPOCH;
        Instant end1 = start1.plus(job1.duration());
        Instant start2 = start1.plus(Duration.ofHours(1));
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
        
        // Clear jobs
        solution.clearJobs();
        
        // Verify all jobs were removed
        assertEquals(0, solution.getScheduledJobs().size());
        assertEquals(Duration.ZERO, solution.getMakespan());
        assertTrue(solution.getMachineAssignments().isEmpty());
    }
    
    @Test
    void testClone() {
        // Create test data
        Machine machine = new Machine("M1", Instant.EPOCH);
        Job job = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        
        // Create solution with job
        Instant start = Instant.EPOCH;
        Instant end = start.plus(job.duration());
        JobSchedulingSolution.ScheduledJob scheduledJob = new JobSchedulingSolution.ScheduledJob(
            job, machine, start, end);
            
        JobSchedulingSolution solution = new JobSchedulingSolution(
            List.of(scheduledJob),
            job.duration(),
            Map.of("M1", List.of(scheduledJob)),
            1
        );
        
        // Clone solution
        JobSchedulingSolution clone = solution.Clone();
        
        // Verify clone has same content
        assertEquals(solution.getScheduledJobs().size(), clone.getScheduledJobs().size());
        assertEquals(solution.getMakespan(), clone.getMakespan());
        assertEquals(solution.getMachineAssignments().size(), clone.getMachineAssignments().size());
        
        // Verify collections are independent
        clone.removeJob(scheduledJob);
        assertEquals(1, solution.getScheduledJobs().size());
        assertEquals(0, clone.getScheduledJobs().size());
    }
    
    @Test
    void testUpdateMakespan() {
        // Create test data
        Machine machine = new Machine("M1", Instant.EPOCH);
        Job job1 = new Job("J1", List.of("M1"), Duration.ofHours(1), List.of());
        Job job2 = new Job("J2", List.of("M1"), Duration.ofHours(2), List.of());
        
        // Create solution with first job
        Instant start1 = Instant.EPOCH;
        Instant end1 = start1.plus(job1.duration());
        JobSchedulingSolution.ScheduledJob scheduledJob1 = new JobSchedulingSolution.ScheduledJob(
            job1, machine, start1, end1);
            
        JobSchedulingSolution solution = new JobSchedulingSolution(
            List.of(scheduledJob1),
            job1.duration(),
            Map.of("M1", List.of(scheduledJob1)),
            2
        );
        
        // Add second job
        Instant start2 = end1;
        Instant end2 = start2.plus(job2.duration());
        JobSchedulingSolution.ScheduledJob scheduledJob2 = new JobSchedulingSolution.ScheduledJob(
            job2, machine, start2, end2);
        solution.addJob(scheduledJob2);
        
        // Verify makespan was updated
        assertEquals(Duration.ofHours(3), solution.getMakespan());
        
        // Remove first job
        solution.removeJob(scheduledJob1);
        
        // Verify makespan was updated
        assertEquals(job2.duration(), solution.getMakespan());
    }
} 
