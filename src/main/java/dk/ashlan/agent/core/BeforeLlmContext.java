package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;
import java.util.Optional;

public final class BeforeLlmContext {
    private final String sessionId;
    private final int stepNumber;
    private final List<LlmMessage> messages;
    private List<LlmMessage> projectedMessages;
    private String optimizationSummary;

    public BeforeLlmContext(String sessionId, int stepNumber, List<LlmMessage> messages) {
        this.sessionId = sessionId;
        this.stepNumber = stepNumber;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public String sessionId() {
        return sessionId;
    }

    public int stepNumber() {
        return stepNumber;
    }

    public List<LlmMessage> messages() {
        return messages;
    }

    public void projectMessages(List<LlmMessage> projectedMessages) {
        this.projectedMessages = projectedMessages == null ? null : List.copyOf(projectedMessages);
    }

    public Optional<List<LlmMessage>> projectedMessages() {
        return Optional.ofNullable(projectedMessages);
    }

    public void setOptimizationSummary(String optimizationSummary) {
        this.optimizationSummary = optimizationSummary;
    }

    public Optional<String> optimizationSummary() {
        return Optional.ofNullable(optimizationSummary);
    }
}
