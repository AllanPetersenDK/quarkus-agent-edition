package dk.ashlan.agent.core;

public record AfterRunContext(
        String sessionId,
        String input,
        AgentRunResult result
) {
}
