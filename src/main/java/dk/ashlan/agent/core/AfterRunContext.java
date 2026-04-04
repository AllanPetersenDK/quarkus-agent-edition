package dk.ashlan.agent.core;

import java.util.List;

public record AfterRunContext(
        String sessionId,
        String input,
        AgentRunResult result,
        List<String> trace
) {
}
