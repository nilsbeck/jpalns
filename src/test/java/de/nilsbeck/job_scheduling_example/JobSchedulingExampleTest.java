package de.nilsbeck.job_scheduling_example;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.nilsbeck.job_scheduling_example.model.Job;
import de.nilsbeck.job_scheduling_example.model.Machine;
import de.nilsbeck.job_scheduling_example.model.Input;

class JobSchedulingExampleTest {

    @Test
    void testJsonParsing() throws Exception {
        // Load and parse the sample problem
        String problemJson = new String(Files.readAllBytes(Paths.get("src/main/java/de/nilsbeck/job_scheduling_example/lta21_sample.json")));
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
        Input problem = mapper.readValue(problemJson, Input.class);

        // Test config
        assertEquals("lta21_sample", problem.config().run().name());

        // Test machines
        List<Machine> machines = problem.modelInput().machines();
        assertEquals(3, machines.size());
        
        Machine machine0 = machines.get(0);
        assertEquals("machine0", machine0.id());
        assertEquals(Instant.parse("2027-02-01T00:00:00Z"), machine0.start());

        // Test jobs
        List<Job> jobs = problem.modelInput().jobs();
        assertEquals(11, jobs.size());

        // Test first job (job0)
        Job job0 = jobs.get(0);
        assertEquals("job0", job0.id());
        assertEquals(List.of("machine0"), job0.requiredResources());
        assertEquals(Duration.parse("PT11H34M"), job0.duration());
        assertTrue(job0.dependencies().isEmpty());

        // Test job with dependencies (job1)
        Job job1 = jobs.get(1);
        assertEquals("job1", job1.id());
        assertEquals(List.of("machine1"), job1.requiredResources());
        assertEquals(Duration.parse("PT12H43M"), job1.duration());
        assertEquals(1, job1.dependencies().size());
        assertEquals("job0", job1.dependencies().get(0).id());

        // Test last job (job10)
        Job job10 = jobs.get(10);
        assertEquals("job10", job10.id());
        assertEquals(List.of("machine1"), job10.requiredResources());
        assertEquals(Duration.parse("PT29M"), job10.duration());
        assertEquals(1, job10.dependencies().size());
        assertEquals("job9", job10.dependencies().get(0).id());
    }
} 
