package dk.ashlan.agent.core;

import java.util.List;

public record AgentRunResult(
        String finalAnswer,
        StopReason stopReason,
        int iterations,
        List<String> trace
) {
}
