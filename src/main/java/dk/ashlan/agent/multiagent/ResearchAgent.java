package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ResearchAgent implements SpecialistAgent {
    @Override
    public String name() {
        return "research";
    }

    @Override
    public boolean supports(AgentTask task) {
        return task.objective().toLowerCase().contains("research") || task.objective().toLowerCase().contains("find");
    }

    @Override
    public AgentTaskResult execute(AgentTask task) {
        String output = "Research summary for: " + task.objective() + ". Key angle: compare sources and capture stable facts.";
        return new AgentTaskResult(
                task.id(),
                Instant.now(),
                task.objective(),
                name(),
                output,
                true,
                "Looks good.",
                "research-specialist",
                "Research specialist draft for: " + task.objective(),
                List.of("chapter9-draft:research")
        );
    }
}
