package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record AgentRunResponse(
        @Schema(description = "Agent answer text.")
        String answer,
        @Schema(description = "Reason the agent stopped.")
        StopReason stopReason,
        @Schema(description = "Number of iterations executed.")
        int iterations,
        @Schema(description = "Execution trace collected during the run.")
        List<String> trace
) {
    public static AgentRunResponse from(AgentRunResult result) {
        return new AgentRunResponse(
                result.finalAnswer(),
                result.stopReason(),
                result.iterations(),
                result.trace()
        );
    }
}
