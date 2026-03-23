package dk.ashlan.agent.multiagent;

public record AgentTaskResult(String agentName, String output, boolean approved, String review) {
}
