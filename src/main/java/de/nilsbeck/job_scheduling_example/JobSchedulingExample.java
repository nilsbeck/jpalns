package de.nilsbeck.job_scheduling_example;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.nilsbeck.job_scheduling_example.model.Input;
import de.nilsbeck.job_scheduling_example.model.ModelInput;

public class JobSchedulingExample { 
    public static void main(String[] args) {
        try {
            // Load and parse the sample problem
            String problemJson = new String(Files.readAllBytes(Paths.get("src/main/java/de/nilsbeck/job_scheduling_example/lta21_sample.json")));
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
            Input input = mapper.readValue(problemJson, Input.class);
            ModelInput problem = input.modelInput();

            // Create and run the solver
            JobSchedulingSolver solver = new JobSchedulingSolver();
            var solution = solver.Solve(problem);
            
            // Print solution details
            System.out.println("Problem: " + input.config().run().name());
            System.out.println("Number of machines: " + problem.machines().size());
            System.out.println("Number of jobs: " + problem.jobs().size());
            System.out.println("\nSolution:");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
