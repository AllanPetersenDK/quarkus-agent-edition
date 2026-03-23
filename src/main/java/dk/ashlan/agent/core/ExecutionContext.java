package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExecutionContext {
    private final String input;
    private final String sessionId;
    private final List<LlmMessage> messages = new ArrayList<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private String finalAnswer;

    public ExecutionContext(String input) {
        this(input, "default");
    }

    public ExecutionContext(String input, String sessionId) {
        this.input = Objects.requireNonNullElse(input, "");
        this.sessionId = Objects.requireNonNullElse(sessionId, "default");
        this.messages.add(LlmMessage.user(this.input));
    }

    public String getInput() {
        return input;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<LlmMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void addAssistantMessage(String content) {
        messages.add(LlmMessage.assistant(content));
    }

    public void addToolMessage(String toolName, String content) {
        messages.add(LlmMessage.tool(toolName, content));
    }

    public void addSystemMessage(String content) {
        messages.add(LlmMessage.system(content));
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public boolean isFinalAnswer() {
        return finalAnswer != null && !finalAnswer.isBlank();
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }
}
