package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

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
        return new AgentTaskResult(name(), "Research summary for: " + task.objective(), true, "Looks good.");
    }
}
