package de.nilsbeck.job_scheduling_example;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.nilsbeck.job_scheduling_example.model.Input;
import de.nilsbeck.job_scheduling_example.model.ModelInput;

public class JobSchedulingExample { 
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -cp <jar> de.nilsbeck.job_scheduling_example.JobSchedulingExample <input_file.json>");
            System.exit(1);
        }
        try {
            // Load and parse the sample problem
            String problemJson = new String(Files.readAllBytes(Paths.get(args[0])));
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
            Input input = mapper.readValue(problemJson, Input.class);
            ModelInput problem = input.modelInput();

            // Create and run the solver
            JobSchedulingSolver solver = new JobSchedulingSolver(100, System.nanoTime());
            var solution = solver.Solve(problem);
            
            // Validate the solution
            var validation = JobSchedulingSolver.validateSolution(solution, problem);
            
            // Print solution details
            System.out.println("Problem: " + input.config().run().name());
            System.out.println("Number of machines: " + problem.machines().size());
            System.out.println("Number of jobs: " + problem.jobs().size());
            System.out.println("\nSolution:");
            System.out.println("Makespan: " + solution.getMakespan().toHours() + " hours");
            System.out.println("Scheduled jobs: " + solution.getScheduledJobs().size() + 
                " of " + solution.getTotalJobs());
            System.out.println("Unassigned jobs: " + solution.getUnassignedJobs());
            
            // Print validation results
            System.out.println("\nValidation:");
            if (validation.isValid()) {
                System.out.println("✓ Solution is valid");
            } else {
                System.out.println("✗ Solution is invalid:");
                System.out.println(validation);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
