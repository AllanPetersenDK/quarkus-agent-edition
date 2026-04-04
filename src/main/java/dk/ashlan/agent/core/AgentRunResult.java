package dk.ashlan.agent.core;

import java.util.List;

public record AgentRunResult(
        String finalAnswer,
        StopReason stopReason,
        int iterations,
        List<String> trace,
        List<PendingToolCall> pendingToolCalls
) {
    public AgentRunResult(String finalAnswer, StopReason stopReason, int iterations, List<String> trace) {
        this(finalAnswer, stopReason, iterations, trace, List.of());
    }
}
