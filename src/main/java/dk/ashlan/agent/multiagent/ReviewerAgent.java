package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

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
        boolean approved = task.context() == null || task.context().length() >= 10;
        return new AgentTaskResult(name(), task.context(), approved, approved ? "Approved." : "Output is too thin.");
    }
}
