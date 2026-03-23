package dk.ashlan.agent.core;

import java.util.List;

public record AgentRunTrace(
        String sessionId,
        List<String> events
) {
}
