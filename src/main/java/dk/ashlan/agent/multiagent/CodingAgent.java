package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

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
        return new AgentTaskResult(name(), "Coding plan for: " + task.objective(), true, "Ready to implement.");
    }
}
