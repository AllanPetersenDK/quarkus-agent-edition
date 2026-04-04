package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class CodingAgent implements SpecialistAgent {
    @Override
    public String name() {
        return "coding";
    }

    @Override
    public boolean supports(AgentTask task) {
        return task.objective().toLowerCase().contains("code") || task.objective().toLowerCase().contains("implement");
    }

    @Override
    public AgentTaskResult execute(AgentTask task) {
        String output = "Coding plan for: " + task.objective() + ". Key steps: define the seam, wire the endpoint, and verify with tests.";
        return new AgentTaskResult(
                task.id(),
                Instant.now(),
                task.objective(),
                name(),
                output,
                true,
                "Ready to implement.",
                "coding-specialist",
                "Coding specialist draft for: " + task.objective(),
                List.of("chapter9-draft:coding")
        );
    }
}
