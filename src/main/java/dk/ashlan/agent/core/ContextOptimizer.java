package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmRequest;
import dk.ashlan.agent.memory.SlidingWindowStrategy;
import dk.ashlan.agent.memory.SummarizationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ContextOptimizer {
    private final SlidingWindowStrategy slidingWindowStrategy;
    private final SummarizationStrategy summarizationStrategy;
    private final int maxTokens;
    private final int slidingWindowSize;
    private final int summaryTailSize;
    private final int toolCompactionChars;

    public ContextOptimizer(
            SlidingWindowStrategy slidingWindowStrategy,
            SummarizationStrategy summarizationStrategy,
            @ConfigProperty(name = "agent.context.max-tokens", defaultValue = "1800") int maxTokens,
            @ConfigProperty(name = "agent.context.sliding-window-size", defaultValue = "8") int slidingWindowSize,
            @ConfigProperty(name = "agent.context.summary-tail-size", defaultValue = "5") int summaryTailSize,
            @ConfigProperty(name = "agent.context.tool-compaction-chars", defaultValue = "240") int toolCompactionChars
    ) {
        this.slidingWindowStrategy = slidingWindowStrategy;
        this.summarizationStrategy = summarizationStrategy;
        this.maxTokens = maxTokens;
        this.slidingWindowSize = slidingWindowSize;
        this.summaryTailSize = summaryTailSize;
        this.toolCompactionChars = toolCompactionChars;
    }

    public ContextOptimizationResult optimize(LlmRequest request) {
        List<LlmMessage> input = request == null || request.messages() == null ? List.of() : List.copyOf(request.messages());
        int originalTokens = new LlmRequest(input).estimatedTokenCount();
        if (originalTokens <= maxTokens) {
            return ContextOptimizationResult.unchanged(input, originalTokens);
        }

        List<LlmMessage> compacted = compactToolMessages(input);
        int compactedTokens = new LlmRequest(compacted).estimatedTokenCount();
        if (compactedTokens <= maxTokens) {
            return new ContextOptimizationResult(List.copyOf(compacted), "compaction", originalTokens, compactedTokens);
        }

        List<LlmMessage> summarized = summarize(compacted);
        int summarizedTokens = new LlmRequest(summarized).estimatedTokenCount();
        return new ContextOptimizationResult(List.copyOf(summarized), "summarization", originalTokens, summarizedTokens);
    }

    private List<LlmMessage> compactToolMessages(List<LlmMessage> messages) {
        List<LlmMessage> compacted = new ArrayList<>(messages.size());
        for (LlmMessage message : messages) {
            if ("tool".equals(message.role())) {
                compacted.add(compactToolMessage(message));
            } else {
                compacted.add(message);
            }
        }
        return compacted;
    }

    private LlmMessage compactToolMessage(LlmMessage message) {
        String content = message.content() == null ? "" : message.content();
        if (content.isBlank() || content.length() <= toolCompactionChars || isLikelyFailure(content)) {
            return message;
        }
        String preview = content.replaceAll("\\s+", " ").trim();
        if (preview.length() > toolCompactionChars) {
            preview = preview.substring(0, toolCompactionChars).trim() + " …";
        }
        String summary = "[compacted tool output; originalChars=" + content.length() + "] " + preview;
        return message.toolCallId() == null
                ? LlmMessage.tool(message.name(), summary)
                : LlmMessage.tool(message.name(), message.toolCallId(), summary);
    }

    private boolean isLikelyFailure(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return normalized.contains("error")
                || normalized.contains("failed")
                || normalized.contains("failure")
                || normalized.contains("not found");
    }

    private List<LlmMessage> summarize(List<LlmMessage> messages) {
        List<LlmMessage> head = leadingSystemMessages(messages);
        int lastUserIndex = lastIndexOfRole(messages, "user");
        int tailStart = lastUserIndex >= head.size()
                ? lastUserIndex
                : Math.max(head.size(), messages.size() - summaryTailSize);
        if (tailStart <= head.size()) {
            return List.copyOf(messages);
        }
        List<LlmMessage> tail = messages.subList(tailStart, messages.size());
        List<LlmMessage> middle = messages.subList(head.size(), tailStart);
        String summary = summarizationStrategy.summarizeConversation(middle);
        if (summary.isBlank()) {
            return slidingWindowStrategy.trimConversation(messages, slidingWindowSize);
        }

        List<LlmMessage> summarized = new ArrayList<>(head.size() + tail.size() + 1);
        summarized.addAll(head);
        summarized.add(LlmMessage.system("Context summary: " + summary));
        summarized.addAll(tail);
        return summarized;
    }

    private List<LlmMessage> leadingSystemMessages(List<LlmMessage> messages) {
        List<LlmMessage> systems = new ArrayList<>();
        for (LlmMessage message : messages) {
            if (!"system".equals(message.role())) {
                break;
            }
            systems.add(message);
        }
        return systems;
    }

    private int lastIndexOfRole(List<LlmMessage> messages, String role) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (role.equals(messages.get(index).role())) {
                return index;
            }
        }
        return -1;
    }
}
