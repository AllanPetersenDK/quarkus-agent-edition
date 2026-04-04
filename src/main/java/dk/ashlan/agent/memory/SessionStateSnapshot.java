package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.PendingToolCall;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.planning.Chapter7ReflectionState;
import dk.ashlan.agent.planning.ExecutionPlan;

import java.util.List;

public record SessionStateSnapshot(
        List<LlmMessage> messages,
        List<PendingToolCall> pendingToolCalls,
        ExecutionPlan chapter7Plan,
        Chapter7ReflectionState chapter7Reflection
) {
    public SessionStateSnapshot {
        messages = messages == null ? List.of() : List.copyOf(messages);
        pendingToolCalls = pendingToolCalls == null ? List.of() : List.copyOf(pendingToolCalls);
        chapter7Plan = chapter7Plan;
        chapter7Reflection = chapter7Reflection;
    }
}
