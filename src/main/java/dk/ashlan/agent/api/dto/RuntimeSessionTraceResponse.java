package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.core.StopReason;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Structured runtime trace response for a session.")
public record RuntimeSessionTraceResponse(
        @Schema(description = "Session identifier.")
        String sessionId,
        @Schema(description = "Structured step history for the requested session.")
        List<AgentStepResponse> steps,
        @Schema(description = "Final answer from the latest final step, if one exists.", nullable = true)
        String finalAnswer,
        @Schema(description = "Stop reason inferred from the latest final step, if one exists.", nullable = true)
        StopReason stopReason
) {
    public static RuntimeSessionTraceResponse from(String sessionId, List<AgentStepResponse> steps) {
        String finalAnswer = null;
        StopReason stopReason = null;
        if (!steps.isEmpty()) {
            AgentStepResponse lastStep = steps.get(steps.size() - 1);
            if (lastStep.isFinal()) {
                finalAnswer = lastStep.finalAnswer();
                stopReason = StopReason.FINAL_ANSWER;
            }
        }
        return new RuntimeSessionTraceResponse(sessionId, steps, finalAnswer, stopReason);
    }
}
