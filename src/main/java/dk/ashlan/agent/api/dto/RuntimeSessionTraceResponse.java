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
        for (int index = steps.size() - 1; index >= 0; index--) {
            AgentStepResponse step = steps.get(index);
            if (step.isFinal()) {
                finalAnswer = step.finalAnswer();
                stopReason = StopReason.FINAL_ANSWER;
                break;
            }
        }
        return new RuntimeSessionTraceResponse(sessionId, steps, finalAnswer, stopReason);
    }
}
