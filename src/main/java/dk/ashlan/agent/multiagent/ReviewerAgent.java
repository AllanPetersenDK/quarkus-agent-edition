package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ReviewerAgent implements SpecialistAgent {
    @Override
    public String name() {
        return "reviewer";
    }

    @Override
    public boolean supports(AgentTask task) {
        return true;
    }

    @Override
    public AgentTaskResult execute(AgentTask task) {
        String output = task.context() == null ? "" : task.context().trim();
        boolean objectiveClear = task.objective() != null && task.objective().trim().length() >= 8;
        boolean substantialOutput = output.length() >= 25;
        boolean approved = objectiveClear && substantialOutput;
        String review = approved
                ? "Reviewer: Approved. Specialist output is concrete enough for the objective."
                : "Reviewer: Rejected. Specialist output is too thin for the objective.";
        return new AgentTaskResult(
                task.id(),
                Instant.now(),
                task.objective(),
                name(),
                output,
                approved,
                review,
                "review-stage",
                "Reviewer assessment for: " + task.objective(),
                List.of("chapter9-review:" + (approved ? "approved" : "rejected"))
        );
    }
}
