package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.PendingToolCall;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SessionState {
    private final String sessionId;
    private final List<LlmMessage> messages = new ArrayList<>();
    private final List<PendingToolCall> pendingToolCalls = new ArrayList<>();
    private final Consumer<SessionState> onChange;

    public SessionState(String sessionId) {
        this(sessionId, List.of(), null);
    }

    SessionState(String sessionId, List<LlmMessage> initialMessages, Consumer<SessionState> onChange) {
        this.sessionId = sessionId;
        this.onChange = onChange;
        this.messages.addAll(initialMessages);
    }

    public String sessionId() {
        return sessionId;
    }

    public synchronized List<LlmMessage> messages() {
        return List.copyOf(messages);
    }

    public synchronized List<PendingToolCall> pendingToolCalls() {
        return List.copyOf(pendingToolCalls);
    }

    public void addUserMessage(String content) {
        addMessage(LlmMessage.user(content));
    }

    public void addAssistantMessage(String content) {
        addMessage(LlmMessage.assistant(content));
    }

    public void addAssistantToolCalls(List<LlmToolCall> toolCalls) {
        addMessage(LlmMessage.assistant(toolCalls));
    }

    public void addToolMessage(String toolName, String content) {
        addMessage(LlmMessage.tool(toolName, content));
    }

    public void addToolMessage(String toolName, String toolCallId, String content) {
        addMessage(LlmMessage.tool(toolName, toolCallId, content));
    }

    public void addMessage(LlmMessage message) {
        Consumer<SessionState> callback;
        synchronized (this) {
            messages.add(message);
            callback = onChange;
        }
        if (callback != null) {
            // The callback runs after the mutation is committed, so persistence does not hold the lock.
            callback.accept(this);
        }
    }

    public void addPendingToolCall(PendingToolCall pendingToolCall) {
        synchronized (this) {
            pendingToolCalls.add(pendingToolCall);
        }
    }

    public void clearPendingToolCalls() {
        synchronized (this) {
            pendingToolCalls.clear();
        }
    }

    public synchronized int size() {
        return messages.size();
    }
}
