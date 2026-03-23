package dk.ashlan.agent.multiagent;

public interface SpecialistAgent {
    String name();

    boolean supports(AgentTask task);

    AgentTaskResult execute(AgentTask task);
}
