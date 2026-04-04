package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.PendingToolCall;
import dk.ashlan.agent.core.StopReason;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgentRunResponse(
        @Schema(description = "Agent answer text.")
        String answer,
        @Schema(description = "Reason the agent stopped.")
        StopReason stopReason,
        @Schema(description = "Number of iterations executed.")
        int iterations,
        @Schema(description = "Execution trace collected during the run.")
        List<String> trace,
        @Schema(description = "Pending tool calls waiting for confirmation when the run pauses.")
        List<PendingToolCallResponse> pendingToolCalls
) {
    public static AgentRunResponse from(AgentRunResult result) {
        return new AgentRunResponse(
                result.finalAnswer(),
                result.stopReason(),
                result.iterations(),
                result.trace(),
                result.pendingToolCalls().stream().map(PendingToolCallResponse::from).toList()
        );
    }

    public record PendingToolCallResponse(
            @Schema(description = "Session identifier.")
            String sessionId,
            @Schema(description = "Step number where the pending call was created.")
            int stepNumber,
            @Schema(description = "Assistant input that led to the pending call.")
            String input,
            @Schema(description = "Pending tool name.")
            String toolName,
            @Schema(description = "Pending tool-call identifier.")
            String toolCallId,
            @Schema(description = "Pending tool-call arguments.")
            Map<String, Object> arguments,
            @Schema(description = "Confirmation message shown to the user.")
            String confirmationMessage
    ) {
        static PendingToolCallResponse from(PendingToolCall pendingToolCall) {
            Map<String, Object> arguments = pendingToolCall.arguments() == null
                    ? Map.of()
                    : new LinkedHashMap<>(pendingToolCall.arguments());
            return new PendingToolCallResponse(
                    pendingToolCall.sessionId(),
                    pendingToolCall.stepNumber(),
                    pendingToolCall.input(),
                    pendingToolCall.toolCall() == null ? "" : pendingToolCall.toolCall().toolName(),
                    pendingToolCall.toolCall() == null ? "" : pendingToolCall.toolCall().callId(),
                    arguments,
                    pendingToolCall.confirmationMessage()
            );
        }
    }
}
