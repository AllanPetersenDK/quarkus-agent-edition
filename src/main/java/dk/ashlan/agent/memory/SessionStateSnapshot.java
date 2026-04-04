package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.PendingToolCall;
import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;

public record SessionStateSnapshot(
        List<LlmMessage> messages,
        List<PendingToolCall> pendingToolCalls
) {
    public SessionStateSnapshot {
        messages = messages == null ? List.of() : List.copyOf(messages);
        pendingToolCalls = pendingToolCalls == null ? List.of() : List.copyOf(pendingToolCalls);
    }
}
