package dk.ashlan.agent.multiagent;

import java.time.Instant;
import java.util.List;

public record AgentTaskResult(
        String runId,
        Instant createdAt,
        String objective,
        String agentName,
        String output,
        boolean approved,
        String review,
        String routeReason,
        String coordinatorSummary,
        List<String> traceEntries
) {
    public AgentTaskResult {
        traceEntries = traceEntries == null ? List.of() : List.copyOf(traceEntries);
    }
}
