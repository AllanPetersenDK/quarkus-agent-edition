package dk.ashlan.agent.api.dto;

import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.AgentTraceEntry;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Structured chapter-4 agent step response with tool calls, tool results, and step-local trace entries.")
public record AgentStepResponse(
        @Schema(description = "Session identifier associated with the step.")
        String sessionId,
        @Schema(description = "One-based step number within the session.")
        int stepNumber,
        @Schema(description = "Assistant message content when the step produced one.", nullable = true)
        String assistantMessage,
        @Schema(description = "Structured tool calls requested by the assistant during the step.")
        List<ToolCallResponse> toolCalls,
        @Schema(description = "Structured tool execution results captured during the step.")
        List<ToolResultResponse> toolResults,
        @Schema(description = "Final answer produced by the step, or null when the step only produced tool calls.", nullable = true)
        String finalAnswer,
        @Schema(description = "True when the step produced a final answer and no further agent loop is required.")
        boolean isFinal,
        @Schema(description = "Step-local structured trace entries for inspection.")
        List<TraceEntryResponse> traceEntries
) {
    public static AgentStepResponse from(AgentStepResult result) {
        return new AgentStepResponse(
                result.sessionId(),
                result.stepNumber(),
                result.assistantMessage(),
                result.toolCalls().stream().map(ToolCallResponse::from).toList(),
                result.toolResults().stream().map(ToolResultResponse::from).toList(),
                result.finalAnswer(),
                result.isFinal(),
                result.traceEntries().stream().map(TraceEntryResponse::from).toList()
        );
    }

    public record ToolCallResponse(
            @Schema(description = "Tool name requested by the assistant.")
            String toolName,
            @Schema(description = "Structured tool-call arguments.")
            Map<String, Object> arguments,
            @Schema(description = "Optional tool-call identifier returned by the model provider.", nullable = true)
            String callId
    ) {
        static ToolCallResponse from(LlmToolCall toolCall) {
            return new ToolCallResponse(toolCall.toolName(), toolCall.arguments(), toolCall.callId());
        }
    }

    public record ToolResultResponse(
            @Schema(description = "Tool name that produced the result.")
            String toolName,
            @Schema(description = "True when the tool execution succeeded.")
            boolean success,
            @Schema(description = "Tool output text.")
            String output,
            @Schema(description = "Structured tool result data.")
            Map<String, Object> data
    ) {
        static ToolResultResponse from(JsonToolResult result) {
            return new ToolResultResponse(result.toolName(), result.success(), result.output(), result.data());
        }
    }

    public record TraceEntryResponse(
            @Schema(description = "Trace entry kind, such as request-prep, step, tool-call, tool-result, or assistant-message.")
            String kind,
            @Schema(description = "Human-readable trace message.")
            String message
    ) {
        static TraceEntryResponse from(AgentTraceEntry entry) {
            return new TraceEntryResponse(entry.kind(), entry.message());
        }
    }
}
