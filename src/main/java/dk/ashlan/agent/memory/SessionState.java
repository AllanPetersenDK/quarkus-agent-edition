package dk.ashlan.agent.memory;

import dk.ashlan.agent.core.PendingToolCall;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.planning.Chapter7ReflectionState;
import dk.ashlan.agent.planning.ExecutionPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SessionState {
    private final String sessionId;
    private final List<LlmMessage> messages = new ArrayList<>();
    private final List<PendingToolCall> pendingToolCalls = new ArrayList<>();
    private ExecutionPlan chapter7Plan;
    private Chapter7ReflectionState chapter7Reflection;
    private final Consumer<SessionState> onChange;

    public SessionState(String sessionId) {
        this(sessionId, List.of(), null);
    }

    public SessionState(String sessionId, List<LlmMessage> initialMessages, List<PendingToolCall> initialPendingToolCalls, Consumer<SessionState> onChange) {
        this(sessionId, initialMessages, initialPendingToolCalls, null, null, onChange);
    }

    public SessionState(
            String sessionId,
            List<LlmMessage> initialMessages,
            List<PendingToolCall> initialPendingToolCalls,
            ExecutionPlan chapter7Plan,
            Chapter7ReflectionState chapter7Reflection,
            Consumer<SessionState> onChange
    ) {
        this.sessionId = sessionId;
        this.onChange = onChange;
        this.messages.addAll(initialMessages == null ? List.of() : initialMessages);
        this.pendingToolCalls.addAll(initialPendingToolCalls == null ? List.of() : initialPendingToolCalls);
        this.chapter7Plan = chapter7Plan;
        this.chapter7Reflection = chapter7Reflection;
    }

    SessionState(String sessionId, List<LlmMessage> initialMessages, Consumer<SessionState> onChange) {
        this(sessionId, initialMessages, List.of(), null, null, onChange);
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

    public synchronized ExecutionPlan chapter7Plan() {
        return chapter7Plan;
    }

    public synchronized Chapter7ReflectionState chapter7Reflection() {
        return chapter7Reflection;
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
        Consumer<SessionState> callback;
        synchronized (this) {
            pendingToolCalls.add(pendingToolCall);
            callback = onChange;
        }
        if (callback != null) {
            callback.accept(this);
        }
    }

    public void clearPendingToolCalls() {
        Consumer<SessionState> callback;
        synchronized (this) {
            if (pendingToolCalls.isEmpty()) {
                return;
            }
            pendingToolCalls.clear();
            callback = onChange;
        }
        if (callback != null) {
            callback.accept(this);
        }
    }

    public void setChapter7Plan(ExecutionPlan chapter7Plan) {
        Consumer<SessionState> callback;
        synchronized (this) {
            this.chapter7Plan = chapter7Plan;
            callback = onChange;
        }
        if (callback != null) {
            callback.accept(this);
        }
    }

    public void setChapter7Reflection(Chapter7ReflectionState chapter7Reflection) {
        Consumer<SessionState> callback;
        synchronized (this) {
            this.chapter7Reflection = chapter7Reflection;
            callback = onChange;
        }
        if (callback != null) {
            callback.accept(this);
        }
    }

    public synchronized int size() {
        return messages.size();
    }
}
