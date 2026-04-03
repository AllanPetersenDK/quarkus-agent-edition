package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;

import java.util.List;

public record AgentStepResult(
        String sessionId,
        int stepNumber,
        String assistantMessage,
        List<LlmToolCall> toolCalls,
        List<JsonToolResult> toolResults,
        String finalAnswer,
        boolean isFinal,
        List<AgentTraceEntry> traceEntries
) {
}
